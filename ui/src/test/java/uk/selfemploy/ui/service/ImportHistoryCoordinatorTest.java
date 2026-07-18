package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.common.enums.ImportAuditType;
import uk.selfemploy.ui.service.ImportHistoryCoordinator.UndoResult;
import uk.selfemploy.ui.viewmodel.ImportHistoryItemViewModel;
import uk.selfemploy.ui.viewmodel.ImportStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ImportHistoryCoordinator")
class ImportHistoryCoordinatorTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID IMPORT_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2025, 6, 10);

    private final SqliteImportAuditRepository auditRepository = mock(SqliteImportAuditRepository.class);
    private final BankTransactionRepository bankRepository = mock(BankTransactionRepository.class);

    private ImportHistoryCoordinator coordinator(SubmittedPeriodIndex submitted) {
        return new ImportHistoryCoordinator(BUSINESS_ID, auditRepository, bankRepository, submitted);
    }

    @Test
    @DisplayName("undo soft-deletes exactly the import's transactions and marks it undone")
    void undoRemovesTransactionsWhenNotSubmitted() {
        BankTransaction a = credit(new BigDecimal("100.00"));
        BankTransaction b = credit(new BigDecimal("50.00"));
        when(auditRepository.findById(IMPORT_ID)).thenReturn(Optional.of(activeAudit()));
        when(bankRepository.findByImportAuditId(IMPORT_ID)).thenReturn(List.of(a, b));
        when(bankRepository.softDelete(any())).thenReturn(true);

        UndoResult result = coordinator(noSubmissions()).undo(IMPORT_ID);

        assertThat(result.success()).isTrue();
        verify(bankRepository).softDelete(a.id());
        verify(bankRepository).softDelete(b.id());
        verify(auditRepository).updateStatus(eq(IMPORT_ID), eq(ImportAuditStatus.UNDONE), any(), any());
    }

    @Test
    @DisplayName("undo is blocked when a transaction was already committed to income or expense")
    void undoBlockedWhenCommitted() {
        BankTransaction committed = credit(new BigDecimal("100.00"))
            .withCategorizedAsIncome(UUID.randomUUID(), Instant.now());
        when(auditRepository.findById(IMPORT_ID)).thenReturn(Optional.of(activeAudit()));
        when(bankRepository.findByImportAuditId(IMPORT_ID)).thenReturn(List.of(committed));

        UndoResult result = coordinator(noSubmissions()).undo(IMPORT_ID);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("income or expenses");
        verify(bankRepository, never()).softDelete(any());
        verify(auditRepository, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("a failed soft-delete leaves the import ACTIVE so it can be retried")
    void undoNotMarkedOnPartialFailure() {
        BankTransaction tx = credit(new BigDecimal("100.00"));
        when(auditRepository.findById(IMPORT_ID)).thenReturn(Optional.of(activeAudit()));
        when(bankRepository.findByImportAuditId(IMPORT_ID)).thenReturn(List.of(tx));
        when(bankRepository.softDelete(any())).thenReturn(false);

        UndoResult result = coordinator(noSubmissions()).undo(IMPORT_ID);

        assertThat(result.success()).isFalse();
        verify(auditRepository, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("undo is blocked when a transaction falls in an already-submitted period")
    void undoBlockedWhenInSubmittedPeriod() {
        BankTransaction inSubmittedPeriod = credit(new BigDecimal("100.00"));
        when(auditRepository.findById(IMPORT_ID)).thenReturn(Optional.of(activeAudit()));
        when(bankRepository.findByImportAuditId(IMPORT_ID)).thenReturn(List.of(inSubmittedPeriod));

        UndoResult result = coordinator(submittedCovering(DATE)).undo(IMPORT_ID);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("submitted");
        verify(bankRepository, never()).softDelete(any());
        verify(auditRepository, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("loadHistory maps audit rows to view models with income/expense split")
    void loadHistoryMapsAuditToViewModel() {
        when(auditRepository.findByBusinessId(BUSINESS_ID)).thenReturn(List.of(activeAudit()));
        when(bankRepository.findByImportAuditId(IMPORT_ID)).thenReturn(List.of(
            credit(new BigDecimal("100.00")), debit(new BigDecimal("40.00"))));

        List<ImportHistoryItemViewModel> history = coordinator(noSubmissions()).loadHistory();

        assertThat(history).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(IMPORT_ID);
            assertThat(item.getStatus()).isEqualTo(ImportStatus.ACTIVE);
            assertThat(item.canUndo()).isTrue();
        });
    }

    private ImportAudit activeAudit() {
        return new ImportAudit(IMPORT_ID, BUSINESS_ID, Instant.now(), "statement.csv", null,
            ImportAuditType.BANK_CSV, 2, 2, 0, List.of(), ImportAuditStatus.ACTIVE,
            null, null, null, null, null, null);
    }

    private BankTransaction credit(BigDecimal amount) {
        return BankTransaction.create(BUSINESS_ID, IMPORT_ID, "csv", DATE, amount,
            "Client payment", null, null, "hash-" + UUID.randomUUID(), Instant.now());
    }

    private BankTransaction debit(BigDecimal amount) {
        return credit(amount.negate());
    }

    private SubmittedPeriodIndex noSubmissions() {
        return new SubmittedPeriodIndex(List.of());
    }

    private SubmittedPeriodIndex submittedCovering(LocalDate date) {
        SubmissionRecord submission = new SubmissionRecord(
            UUID.randomUUID().toString(), BUSINESS_ID.toString(), "SA103", 2025,
            date.minusDays(30), date.plusDays(30),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            "ACCEPTED", null, null, Instant.now());
        return new SubmittedPeriodIndex(List.of(submission));
    }
}
