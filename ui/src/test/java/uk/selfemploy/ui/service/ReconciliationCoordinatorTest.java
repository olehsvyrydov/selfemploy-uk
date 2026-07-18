package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.reconciliation.MatchTier;
import uk.selfemploy.core.reconciliation.ReconciliationMatch;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.service.ReconciliationCoordinator.ReconciliationSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationCoordinator")
class ReconciliationCoordinatorTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final TaxYear TAX_YEAR = TaxYear.of(2025);
    private static final LocalDate DATE = LocalDate.of(2025, 6, 10);

    private final IncomeService incomeService = mock(IncomeService.class);
    private final ExpenseService expenseService = mock(ExpenseService.class);
    private final ReconciliationMatchRepository matchRepository = mock(ReconciliationMatchRepository.class);

    private ReconciliationCoordinator coordinator(List<BankTransaction> bankTransactions) {
        return new ReconciliationCoordinator(BUSINESS_ID, incomeService, expenseService,
            matchRepository, () -> bankTransactions);
    }

    @Test
    @DisplayName("totals and counts come from the tax year's real records; no bank data means no duplicates")
    void computesSummaryFromRecords() {
        when(incomeService.findByTaxYear(BUSINESS_ID, TAX_YEAR))
            .thenReturn(List.of(income(new BigDecimal("1850.00"), "Client payment")));
        when(expenseService.findByTaxYear(BUSINESS_ID, TAX_YEAR))
            .thenReturn(List.of(expense(new BigDecimal("500.00"))));
        when(matchRepository.findByBusinessId(BUSINESS_ID)).thenReturn(List.of());

        ReconciliationSummary summary = coordinator(List.of()).reconcile(TAX_YEAR);

        assertThat(summary.totalIncome()).isEqualByComparingTo("1850.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("500.00");
        assertThat(summary.incomeCount()).isEqualTo(1);
        assertThat(summary.expenseCount()).isEqualTo(1);
        assertThat(summary.duplicateCount()).isZero();
        assertThat(summary.issues()).isEmpty();
        verify(matchRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("an imported bank credit matching a manual income is reported as a duplicate and persisted")
    void reportsAndPersistsMatch() {
        Income manualIncome = income(new BigDecimal("1850.00"), "Client payment");
        when(incomeService.findByTaxYear(BUSINESS_ID, TAX_YEAR)).thenReturn(List.of(manualIncome));
        when(expenseService.findByTaxYear(BUSINESS_ID, TAX_YEAR)).thenReturn(List.of());
        when(matchRepository.findByBusinessId(BUSINESS_ID)).thenReturn(List.of());
        when(matchRepository.countUnresolvedByBusinessId(BUSINESS_ID)).thenReturn(1L);

        BankTransaction credit = bankCredit(new BigDecimal("1850.00"), "Client payment");

        ReconciliationSummary summary = coordinator(List.of(credit)).reconcile(TAX_YEAR);

        assertThat(summary.duplicateCount()).isEqualTo(1);
        assertThat(summary.issues()).singleElement()
            .satisfies(issue -> assertThat(issue.getAffectedCount()).isEqualTo(1));
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(
            list -> list != null && list.size() == 1));
    }

    @Test
    @DisplayName("a match already recorded is not re-persisted, preserving its resolution")
    void doesNotOverwriteExistingMatch() {
        Income manualIncome = income(new BigDecimal("1850.00"), "Client payment");
        when(incomeService.findByTaxYear(BUSINESS_ID, TAX_YEAR)).thenReturn(List.of(manualIncome));
        when(expenseService.findByTaxYear(BUSINESS_ID, TAX_YEAR)).thenReturn(List.of());

        BankTransaction credit = bankCredit(new BigDecimal("1850.00"), "Client payment");
        ReconciliationMatch existing = ReconciliationMatch.create(
            credit.id(), manualIncome.id(), "INCOME", 1.0, MatchTier.EXACT, BUSINESS_ID, java.time.Instant.now());
        when(matchRepository.findByBusinessId(BUSINESS_ID)).thenReturn(List.of(existing));
        when(matchRepository.countUnresolvedByBusinessId(BUSINESS_ID)).thenReturn(0L);

        ReconciliationSummary summary = coordinator(List.of(credit)).reconcile(TAX_YEAR);

        // The pair is already recorded, so nothing new is saved and the (dismissed) match stays out
        // of the count.
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(List::isEmpty));
        assertThat(summary.duplicateCount()).isZero();
    }

    private Income income(BigDecimal amount, String description) {
        return Income.create(BUSINESS_ID, DATE, amount, description, IncomeCategory.SALES, "ref");
    }

    private Expense expense(BigDecimal amount) {
        return Expense.create(BUSINESS_ID, DATE, amount, "Office supplies",
            ExpenseCategory.OFFICE_COSTS, null, "notes");
    }

    private BankTransaction bankCredit(BigDecimal amount, String description) {
        return BankTransaction.create(BUSINESS_ID, UUID.randomUUID(), "csv", DATE, amount,
            description, "1234", "bt-1", "hash-1", Instant.now());
    }
}
