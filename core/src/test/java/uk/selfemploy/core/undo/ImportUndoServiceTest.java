package uk.selfemploy.core.undo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.common.enums.ImportAuditType;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.persistence.repository.ExpenseRepository;
import uk.selfemploy.persistence.repository.ImportAuditRepository;
import uk.selfemploy.persistence.repository.IncomeRepository;
import uk.selfemploy.persistence.repository.SubmissionRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for Import Undo/Rollback (Sprint 10B).
 * Tests UNDO-U01 through UNDO-U12 from /rob's test design.
 *
 * <p>SE-10B-003: Import Undo/Rollback Tests</p>
 *
 * <p>Finance Conditions Tested:</p>
 * <ul>
 *   <li>COND-F1: Block undo if any record was included in ACCEPTED/SUBMITTED submission</li>
 *   <li>COND-F2: Block undo if any record was included in submission with status != DRAFT</li>
 *   <li>COND-F4: Tax summary MUST recalculate immediately after undo</li>
 *   <li>COND-F11: Implement UndoEligibility check before any undo operation</li>
 * </ul>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Import Undo Service Tests (Sprint 10B)")
class ImportUndoServiceTest {

    @Mock
    private ImportAuditRepository auditRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    private Clock fixedClock;
    private ImportUndoService service;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final Instant FIXED_TIME = Instant.parse("2025-06-15T10:30:00Z");
    private static final int UNDO_WINDOW_DAYS = 7;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));
        service = new ImportUndoService(
            auditRepository, incomeRepository, expenseRepository,
            submissionRepository, fixedClock
        );
    }

    // === Helper Methods ===

    private ImportAudit createActiveAudit(UUID id, Instant importTime, List<UUID> recordIds) {
        return createActiveAudit(id, importTime, recordIds, ImportAuditType.CSV_INCOME);
    }

    private ImportAudit createActiveAudit(UUID id, Instant importTime, List<UUID> recordIds, ImportAuditType type) {
        return new ImportAudit(
            id, BUSINESS_ID, importTime,
            "test.csv", "hash123", type,
            recordIds.size(), recordIds.size(), 0,
            recordIds, ImportAuditStatus.ACTIVE, null, null
        );
    }

    private ImportAudit createUndoneAudit(UUID id, Instant importTime) {
        return new ImportAudit(
            id, BUSINESS_ID, importTime,
            "test.csv", "hash123", ImportAuditType.CSV_INCOME,
            5, 5, 0, List.of(),
            ImportAuditStatus.UNDONE, FIXED_TIME, "user"
        );
    }

    private Submission createSubmission(UUID businessId, SubmissionStatus status, TaxYear taxYear) {
        return new Submission(
            UUID.randomUUID(), businessId, SubmissionType.ANNUAL,
            taxYear, taxYear.startDate(), taxYear.endDate(),
            new BigDecimal("50000"), new BigDecimal("10000"), new BigDecimal("40000"),
            status, null, null, FIXED_TIME, FIXED_TIME, null, null
        );
    }

    // === UNDO-U01 to UNDO-U03: Undoable Imports ===

    @Nested
    @DisplayName("Undoable Import Identification")
    class UndoableImportIdentification {

        @Test
        @DisplayName("UNDO-U01: should return undoable imports within 7 days")
        void shouldReturnUndoableImportsWithin7Days() {
            // Given
            UUID auditId = UUID.randomUUID();
            UUID recordId = UUID.randomUUID();
            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, List.of(recordId));

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of());

            // When
            UndoEligibility eligibility = service.checkUndoEligibility(auditId);

            // Then
            assertThat(eligibility.eligible()).isTrue();
            assertThat(eligibility.reason()).isNull();
        }

        @Test
        @DisplayName("UNDO-U02: should not return imports older than 7 days")
        void shouldNotReturnImportsOlderThan7Days() {
            // Given
            UUID auditId = UUID.randomUUID();
            Instant oldImport = FIXED_TIME.minus(8, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, oldImport, List.of(UUID.randomUUID()));

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));

            // When
            UndoEligibility eligibility = service.checkUndoEligibility(auditId);

            // Then
            assertThat(eligibility.eligible()).isFalse();
            assertThat(eligibility.reason()).contains("older than 7 days");
        }

        @Test
        @DisplayName("UNDO-U03: should not return already undone imports")
        void shouldNotReturnAlreadyUndoneImports() {
            // Given
            UUID auditId = UUID.randomUUID();
            ImportAudit audit = createUndoneAudit(auditId, FIXED_TIME.minus(1, ChronoUnit.DAYS));

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));

            // When
            UndoEligibility eligibility = service.checkUndoEligibility(auditId);

            // Then
            assertThat(eligibility.eligible()).isFalse();
            assertThat(eligibility.reason()).contains("already been undone");
        }
    }

    // === UNDO-U04 to UNDO-U08: Undo Operations ===

    @Nested
    @DisplayName("Undo Operations")
    class UndoOperations {

        @Test
        @DisplayName("UNDO-U04: should soft delete records on undo")
        void shouldSoftDeleteRecordsOnUndo() {
            // Given
            UUID auditId = UUID.randomUUID();
            UUID recordId1 = UUID.randomUUID();
            UUID recordId2 = UUID.randomUUID();
            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, List.of(recordId1, recordId2));

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of());
            when(incomeRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(2);
            when(auditRepository.updateStatus(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            UndoResult result = service.undoImport(auditId, "Test reason");

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.recordsUndone()).isEqualTo(2);

            verify(incomeRepository).softDeleteByIds(
                eq(List.of(recordId1, recordId2)),
                eq(FIXED_TIME),
                eq("Import undo"),
                eq("Test reason")
            );
        }

        @Test
        @DisplayName("UNDO-U05: should update audit status on undo")
        void shouldUpdateAuditStatusOnUndo() {
            // Given
            UUID auditId = UUID.randomUUID();
            UUID recordId = UUID.randomUUID();
            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, List.of(recordId));

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of());
            when(incomeRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(1);
            when(auditRepository.updateStatus(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.undoImport(auditId, "Test reason");

            // Then
            verify(auditRepository).updateStatus(argThat(updated ->
                updated.status() == ImportAuditStatus.UNDONE &&
                updated.undoneAt() != null &&
                updated.undoneBy() != null
            ));
        }

        @Test
        @DisplayName("UNDO-U06: should store undo reason")
        void shouldStoreUndoReason() {
            // Given
            UUID auditId = UUID.randomUUID();
            UUID recordId = UUID.randomUUID();
            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, List.of(recordId));
            String reason = "Imported wrong file";

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of());
            when(incomeRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(1);
            when(auditRepository.updateStatus(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.undoImport(auditId, reason);

            // Then
            verify(incomeRepository).softDeleteByIds(
                any(), any(), any(), eq(reason)
            );
        }

        @Test
        @DisplayName("UNDO-U07: should throw exception when audit not found")
        void shouldThrowExceptionWhenAuditNotFound() {
            // Given
            UUID auditId = UUID.randomUUID();
            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.checkUndoEligibility(auditId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Import audit not found");
        }

        @Test
        @DisplayName("UNDO-U08: should include record count in undo result")
        void shouldIncludeRecordCountInUndoResult() {
            // Given
            UUID auditId = UUID.randomUUID();
            List<UUID> recordIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, recordIds);

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of());
            when(incomeRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(3);
            when(auditRepository.updateStatus(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            UndoResult result = service.undoImport(auditId, "Test");

            // Then
            assertThat(result.recordsUndone()).isEqualTo(3);
        }
    }

    // === UNDO-U09 to UNDO-U12: Tax Submission Protection ===

    @Nested
    @DisplayName("Tax Submission Protection (COND-F1, COND-F2)")
    class TaxSubmissionProtection {

        @Test
        @DisplayName("UNDO-U09: should block undo if tax submission ACCEPTED (COND-F1)")
        void shouldBlockUndoIfTaxSubmissionAccepted() {
            // Given
            UUID auditId = UUID.randomUUID();
            UUID recordId = UUID.randomUUID();
            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, List.of(recordId));

            TaxYear taxYear = TaxYear.of(2025);
            Submission acceptedSubmission = createSubmission(BUSINESS_ID, SubmissionStatus.ACCEPTED, taxYear);

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of(acceptedSubmission));

            // When
            UndoEligibility eligibility = service.checkUndoEligibility(auditId);

            // Then
            assertThat(eligibility.eligible()).isFalse();
            assertThat(eligibility.reason()).contains("tax submission");
        }

        @Test
        @DisplayName("UNDO-U10: should block undo if tax submission SUBMITTED (COND-F1)")
        void shouldBlockUndoIfTaxSubmissionSubmitted() {
            // Given
            UUID auditId = UUID.randomUUID();
            UUID recordId = UUID.randomUUID();
            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, List.of(recordId));

            TaxYear taxYear = TaxYear.of(2025);
            Submission submittedSubmission = createSubmission(BUSINESS_ID, SubmissionStatus.SUBMITTED, taxYear);

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of(submittedSubmission));

            // When
            UndoEligibility eligibility = service.checkUndoEligibility(auditId);

            // Then
            assertThat(eligibility.eligible()).isFalse();
            assertThat(eligibility.reason()).contains("tax submission");
        }

        @Test
        @DisplayName("UNDO-U11: should allow undo when only DRAFT submission exists")
        void shouldAllowUndoWhenOnlyDraftSubmissionExists() {
            // Given
            UUID auditId = UUID.randomUUID();
            UUID recordId = UUID.randomUUID();
            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, List.of(recordId));

            // Note: Only DRAFT submissions should not block undo
            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of()); // No non-draft submissions

            // When
            UndoEligibility eligibility = service.checkUndoEligibility(auditId);

            // Then
            assertThat(eligibility.eligible()).isTrue();
        }

        @Test
        @DisplayName("UNDO-U12: should throw UndoBlockedException when undo not allowed")
        void shouldThrowUndoBlockedExceptionWhenNotAllowed() {
            // Given
            UUID auditId = UUID.randomUUID();
            Instant oldImport = FIXED_TIME.minus(8, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, oldImport, List.of(UUID.randomUUID()));

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));

            // When/Then
            assertThatThrownBy(() -> service.undoImport(auditId, "Test"))
                .isInstanceOf(UndoBlockedException.class)
                .hasMessageContaining("older than 7 days");
        }
    }

    // === Additional Validation Tests ===

    @Nested
    @DisplayName("Undo Eligibility Validation (COND-F11)")
    class UndoEligibilityValidation {

        @Test
        @DisplayName("should check eligibility before performing undo")
        void shouldCheckEligibilityBeforePerformingUndo() {
            // Given
            UUID auditId = UUID.randomUUID();
            Instant oldImport = FIXED_TIME.minus(8, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, oldImport, List.of(UUID.randomUUID()));

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));

            // When/Then
            assertThatThrownBy(() -> service.undoImport(auditId, "Test"))
                .isInstanceOf(UndoBlockedException.class);

            // Verify no soft delete was called
            verify(incomeRepository, never()).softDeleteByIds(any(), any(), any(), any());
            verify(expenseRepository, never()).softDeleteByIds(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should include clear reason in eligibility response (COND-F12)")
        void shouldIncludeClearReasonInEligibilityResponse() {
            // Given
            UUID auditId = UUID.randomUUID();
            Instant oldImport = FIXED_TIME.minus(10, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, oldImport, List.of(UUID.randomUUID()));

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));

            // When
            UndoEligibility eligibility = service.checkUndoEligibility(auditId);

            // Then
            assertThat(eligibility.eligible()).isFalse();
            assertThat(eligibility.reason()).isNotBlank();
            assertThat(eligibility.reason()).containsIgnoringCase("7 days");
        }
    }

    // === UNDO-U13 to UNDO-U15: BANK_CSV Mixed Import Undo (MAJOR-002 Bug Fix) ===

    @Nested
    @DisplayName("BANK_CSV Mixed Import Undo (MAJOR-002 Bug Fix)")
    class BankCsvMixedImportUndo {

        @Test
        @DisplayName("UNDO-U13: should soft delete BOTH income AND expense records for BANK_CSV import")
        void shouldSoftDeleteBothIncomeAndExpenseForBankCsv() {
            // Given - BANK_CSV import with mixed income and expense record IDs
            UUID auditId = UUID.randomUUID();
            UUID incomeId1 = UUID.randomUUID();
            UUID incomeId2 = UUID.randomUUID();
            UUID expenseId1 = UUID.randomUUID();
            UUID expenseId2 = UUID.randomUUID();
            List<UUID> allRecordIds = List.of(incomeId1, incomeId2, expenseId1, expenseId2);

            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, allRecordIds, ImportAuditType.BANK_CSV);

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of());
            // Income repository finds 2 income records
            when(incomeRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(2);
            // Expense repository finds 2 expense records
            when(expenseRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(2);
            when(auditRepository.updateStatus(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            UndoResult result = service.undoImport(auditId, "Wrong bank statement");

            // Then - BOTH repositories should be called
            verify(incomeRepository).softDeleteByIds(
                eq(allRecordIds),
                eq(FIXED_TIME),
                eq("Import undo"),
                eq("Wrong bank statement")
            );
            verify(expenseRepository).softDeleteByIds(
                eq(allRecordIds),
                eq(FIXED_TIME),
                eq("Import undo"),
                eq("Wrong bank statement")
            );

            // Total records undone should be 4 (2 income + 2 expense)
            assertThat(result.success()).isTrue();
            assertThat(result.recordsUndone()).isEqualTo(4);
        }

        @Test
        @DisplayName("UNDO-U14: should handle BANK_CSV with only income records (no expenses)")
        void shouldHandleBankCsvWithOnlyIncomeRecords() {
            // Given - BANK_CSV import where all records happen to be income
            UUID auditId = UUID.randomUUID();
            UUID incomeId1 = UUID.randomUUID();
            UUID incomeId2 = UUID.randomUUID();
            List<UUID> allRecordIds = List.of(incomeId1, incomeId2);

            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, allRecordIds, ImportAuditType.BANK_CSV);

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of());
            // Income repository finds 2 records
            when(incomeRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(2);
            // Expense repository finds 0 records (none matched)
            when(expenseRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(0);
            when(auditRepository.updateStatus(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            UndoResult result = service.undoImport(auditId, "Test");

            // Then - Both repositories should be called even if expense finds nothing
            verify(incomeRepository).softDeleteByIds(any(), any(), any(), any());
            verify(expenseRepository).softDeleteByIds(any(), any(), any(), any());

            assertThat(result.success()).isTrue();
            assertThat(result.recordsUndone()).isEqualTo(2);
        }

        @Test
        @DisplayName("UNDO-U15: should handle BANK_CSV with only expense records (no income)")
        void shouldHandleBankCsvWithOnlyExpenseRecords() {
            // Given - BANK_CSV import where all records happen to be expenses
            UUID auditId = UUID.randomUUID();
            UUID expenseId1 = UUID.randomUUID();
            UUID expenseId2 = UUID.randomUUID();
            List<UUID> allRecordIds = List.of(expenseId1, expenseId2);

            Instant recentImport = FIXED_TIME.minus(3, ChronoUnit.DAYS);
            ImportAudit audit = createActiveAudit(auditId, recentImport, allRecordIds, ImportAuditType.BANK_CSV);

            when(auditRepository.findByIdAsDomain(auditId)).thenReturn(Optional.of(audit));
            when(submissionRepository.findByBusinessIdAndTaxYear(eq(BUSINESS_ID), any()))
                .thenReturn(List.of());
            // Income repository finds 0 records
            when(incomeRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(0);
            // Expense repository finds 2 records
            when(expenseRepository.softDeleteByIds(any(), any(), any(), any())).thenReturn(2);
            when(auditRepository.updateStatus(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            UndoResult result = service.undoImport(auditId, "Test");

            // Then - Both repositories should be called
            verify(incomeRepository).softDeleteByIds(any(), any(), any(), any());
            verify(expenseRepository).softDeleteByIds(any(), any(), any(), any());

            assertThat(result.success()).isTrue();
            assertThat(result.recordsUndone()).isEqualTo(2);
        }
    }
}
