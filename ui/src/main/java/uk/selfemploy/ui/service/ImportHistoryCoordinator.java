package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.ui.viewmodel.ImportHistoryItemViewModel;
import uk.selfemploy.ui.viewmodel.ImportStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Backs the Import History screen: lists a business's imports from the audit trail and undoes an
 * import by soft-deleting exactly the transactions it staged. Undo is refused when any of those
 * transactions fall inside a submitted (locked) tax period, so a submitted return can never be
 * silently altered.
 */
public class ImportHistoryCoordinator {

    private final UUID businessId;
    private final SqliteImportAuditRepository auditRepository;
    private final BankTransactionRepository bankRepository;
    private final SubmittedPeriodIndex submittedPeriods;

    public ImportHistoryCoordinator(UUID businessId,
                                    SqliteImportAuditRepository auditRepository,
                                    BankTransactionRepository bankRepository,
                                    SubmittedPeriodIndex submittedPeriods) {
        this.businessId = businessId;
        this.auditRepository = auditRepository;
        this.bankRepository = bankRepository;
        this.submittedPeriods = submittedPeriods;
    }

    /** Lists the business's imports, newest first, as view models for the history screen. */
    public List<ImportHistoryItemViewModel> loadHistory() {
        return auditRepository.findByBusinessId(businessId).stream()
            .map(this::toViewModel)
            .toList();
    }

    /**
     * Undoes an import by soft-deleting the transactions it staged, unless any of them sit in a
     * submitted tax period.
     */
    public UndoResult undo(UUID importId) {
        Optional<ImportAudit> found = auditRepository.findById(importId);
        if (found.isEmpty()) {
            return UndoResult.failure("This import no longer exists.");
        }
        ImportAudit audit = found.get();
        if (audit.status() != ImportAuditStatus.ACTIVE) {
            return UndoResult.failure("This import was already undone.");
        }

        List<BankTransaction> staged = bankRepository.findByImportAuditId(importId);
        Optional<SubmissionRecord> lock = firstSubmittedLock(staged);
        if (lock.isPresent()) {
            return UndoResult.failure(
                "Cannot undo — some of these records are in a tax period you have already submitted.");
        }

        for (BankTransaction tx : staged) {
            bankRepository.softDelete(tx.id());
        }
        auditRepository.updateStatus(importId, ImportAuditStatus.UNDONE, Instant.now(), "You");
        return UndoResult.success(staged.size());
    }

    private Optional<SubmissionRecord> firstSubmittedLock(List<BankTransaction> staged) {
        return staged.stream()
            .map(tx -> submittedPeriods.coveringSubmission(tx.date()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private ImportHistoryItemViewModel toViewModel(ImportAudit audit) {
        List<BankTransaction> staged = bankRepository.findByImportAuditId(audit.id());

        int incomeRecords = 0;
        int expenseRecords = 0;
        BigDecimal incomeTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;
        LocalDateTime taxSubmissionDate = null;
        for (BankTransaction tx : staged) {
            if (tx.amount().signum() >= 0) {
                incomeRecords++;
                incomeTotal = incomeTotal.add(tx.amount());
            } else {
                expenseRecords++;
                expenseTotal = expenseTotal.add(tx.amount().abs());
            }
            if (taxSubmissionDate == null) {
                taxSubmissionDate = submittedPeriods.coveringSubmission(tx.date())
                    .map(s -> LocalDateTime.ofInstant(s.submittedAt(), ZoneId.systemDefault()))
                    .orElse(null);
            }
        }

        ImportStatus status = audit.status() == ImportAuditStatus.UNDONE
            ? ImportStatus.UNDONE : ImportStatus.ACTIVE;
        LocalDateTime importDateTime = LocalDateTime.ofInstant(audit.importTimestamp(), ZoneId.systemDefault());
        LocalDateTime undoneDateTime = audit.undoneAt() != null
            ? LocalDateTime.ofInstant(audit.undoneAt(), ZoneId.systemDefault()) : null;

        return new ImportHistoryItemViewModel(
            audit.id(), importDateTime, audit.fileName(), audit.originalFilePath(), "Bank CSV",
            incomeRecords, expenseRecords, incomeTotal, expenseTotal,
            status, undoneDateTime, taxSubmissionDate);
    }

    /** Outcome of an undo attempt. */
    public record UndoResult(boolean success, String message) {
        static UndoResult success(int removed) {
            return new UndoResult(true, "Removed " + removed + " imported transaction(s).");
        }

        static UndoResult failure(String reason) {
            return new UndoResult(false, reason);
        }
    }
}
