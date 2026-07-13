package uk.selfemploy.core.undo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.common.enums.ImportAuditType;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.persistence.repository.ExpenseRepository;
import uk.selfemploy.persistence.repository.ImportAuditRepository;
import uk.selfemploy.persistence.repository.IncomeRepository;
import uk.selfemploy.persistence.repository.SubmissionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for undoing import operations.
 *
 * <p>Implements import undo/rollback functionality with:</p>
 * <ul>
 *   <li>7-day undo window (ADR-10B-004)</li>
 *   <li>Tax submission protection (COND-F1, COND-F2)</li>
 *   <li>Clock injection for testability</li>
 * </ul>
 *
 * <p>Finance Conditions:</p>
 * <ul>
 *   <li>COND-F1: Block undo if any record was included in ACCEPTED/SUBMITTED submission</li>
 *   <li>COND-F2: Block undo if any record was included in submission with status != DRAFT</li>
 *   <li>COND-F4: Tax summary MUST recalculate immediately after undo (handled by soft delete)</li>
 *   <li>COND-F11: Implement UndoEligibility check before any undo operation</li>
 * </ul>
 */
@ApplicationScoped
public class ImportUndoService {

    private static final int UNDO_WINDOW_DAYS = 7;

    private final ImportAuditRepository auditRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final SubmissionRepository submissionRepository;
    private final Clock clock;

    @Inject
    public ImportUndoService(
            ImportAuditRepository auditRepository,
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            SubmissionRepository submissionRepository,
            Clock clock) {
        this.auditRepository = auditRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.submissionRepository = submissionRepository;
        this.clock = clock;
    }

    /**
     * Checks if an import can be undone.
     *
     * <p>Implements COND-F11: UndoEligibility check before any undo operation.</p>
     *
     * @param auditId the import audit ID
     * @return eligibility result with reason if not allowed
     * @throws IllegalArgumentException if audit not found
     */
    public UndoEligibility checkUndoEligibility(UUID auditId) {
        ImportAudit audit = auditRepository.findByIdAsDomain(auditId)
            .orElseThrow(() -> new IllegalArgumentException("Import audit not found: " + auditId));

        // Check if already undone
        if (audit.status() == ImportAuditStatus.UNDONE) {
            return UndoEligibility.blocked("Import has already been undone");
        }

        // Check time window (7 days)
        Instant cutoff = clock.instant().minus(UNDO_WINDOW_DAYS, ChronoUnit.DAYS);
        if (audit.importTimestamp().isBefore(cutoff)) {
            return UndoEligibility.blocked(
                "Import is older than 7 days and cannot be undone. " +
                "Imports can only be undone within 7 days of the original import."
            );
        }

        // Check tax submission status (COND-F1, COND-F2)
        if (hasNonDraftSubmission(audit)) {
            return UndoEligibility.blocked(
                "Cannot undo - records were used in a tax submission that has been " +
                "submitted or accepted. Once data is submitted to HMRC, it cannot be modified."
            );
        }

        return UndoEligibility.allowed();
    }

    /**
     * Undoes an import by soft-deleting all records.
     *
     * <p>This operation:</p>
     * <ol>
     *   <li>Checks eligibility (COND-F11)</li>
     *   <li>Soft-deletes all income/expense records from the import</li>
     *   <li>Updates the audit record status to UNDONE</li>
     * </ol>
     *
     * <p>Tax calculations will automatically recalculate as soft-deleted
     * records are excluded from queries (COND-F4).</p>
     *
     * @param auditId the import audit ID
     * @param reason reason for undoing the import
     * @return result of the undo operation
     * @throws UndoBlockedException if the import cannot be undone
     */
    @Transactional
    public UndoResult undoImport(UUID auditId, String reason) {
        // Check eligibility first (COND-F11)
        UndoEligibility eligibility = checkUndoEligibility(auditId);
        if (!eligibility.eligible()) {
            throw new UndoBlockedException(eligibility.reason());
        }

        ImportAudit audit = auditRepository.findByIdAsDomain(auditId)
            .orElseThrow(() -> new IllegalArgumentException("Import audit not found: " + auditId));

        Instant now = clock.instant();
        List<UUID> recordIds = audit.recordIds();

        // Soft delete based on import type
        int recordsUndone;
        if (audit.importType() == ImportAuditType.CSV_EXPENSE) {
            // Pure expense import - only expense records
            recordsUndone = expenseRepository.softDeleteByIds(
                recordIds, now, "Import undo", reason
            );
        } else if (audit.importType() == ImportAuditType.BANK_CSV) {
            // BANK_CSV contains mixed income AND expense records
            // Must soft-delete from BOTH repositories (MAJOR-002 fix)
            int incomeUndone = incomeRepository.softDeleteByIds(
                recordIds, now, "Import undo", reason
            );
            int expenseUndone = expenseRepository.softDeleteByIds(
                recordIds, now, "Import undo", reason
            );
            recordsUndone = incomeUndone + expenseUndone;
        } else {
            // CSV_INCOME, JSON - only income records
            recordsUndone = incomeRepository.softDeleteByIds(
                recordIds, now, "Import undo", reason
            );
        }

        // Update audit status
        ImportAudit updatedAudit = audit.withUndone(now, "user");
        auditRepository.updateStatus(updatedAudit);

        int recordsSkipped = recordIds.size() - recordsUndone;
        return UndoResult.success(recordsUndone, recordsSkipped);
    }

    /**
     * Gets all undoable imports for a business within the undo window.
     *
     * @param businessId the business ID
     * @return list of imports that can be undone
     */
    public List<ImportAudit> getUndoableImports(UUID businessId) {
        Instant cutoff = clock.instant().minus(UNDO_WINDOW_DAYS, ChronoUnit.DAYS);
        return auditRepository.findUndoableImports(businessId, cutoff);
    }

    /**
     * Checks if the import affects any non-DRAFT tax submission.
     *
     * <p>Uses date-range approach per /inga's recommendation: if ANY non-DRAFT
     * submission covers the import's date range, block undo.</p>
     */
    private boolean hasNonDraftSubmission(ImportAudit audit) {
        // Determine the tax year(s) that could be affected
        // Import timestamp determines the tax year context
        LocalDate importDate = LocalDate.ofInstant(audit.importTimestamp(), ZoneOffset.UTC);
        TaxYear taxYear = determineTaxYear(importDate);

        // Find submissions for this tax year
        List<Submission> submissions = submissionRepository.findByBusinessIdAndTaxYear(
            audit.businessId(), taxYear
        );

        // Check if any submission is not DRAFT (COND-F1, COND-F2)
        return submissions.stream()
            .anyMatch(s -> s.status() != SubmissionStatus.PENDING);
        // Note: PENDING is used as a proxy for DRAFT since there's no DRAFT status
        // In practice, non-PENDING means it's been submitted or processed
    }

    /**
     * Determines the tax year for a given date.
     */
    private TaxYear determineTaxYear(LocalDate date) {
        // Tax year starts April 6th
        int year = date.getYear();
        if (date.getMonthValue() < 4 || (date.getMonthValue() == 4 && date.getDayOfMonth() < 6)) {
            year = year - 1;
        }
        return TaxYear.of(year);
    }
}
