package uk.selfemploy.core.reconciliation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ReconciliationService - cross-source duplicate detection.
 *
 * Tests cover:
 * - 4-tier matching (LINKED, EXACT, LIKELY, POSSIBLE)
 * - Direction-aware matching (income-to-income, expense-to-expense only)
 * - Business ID scoping
 * - Absolute amount comparison
 * - Tolerance rules (1% or GBP 1.00, whichever is greater)
 */
@DisplayName("ReconciliationService Tests")
class ReconciliationServiceTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID OTHER_BUSINESS_ID = UUID.randomUUID();
    private static final UUID IMPORT_AUDIT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");
    private static final LocalDate TEST_DATE = LocalDate.of(2025, 6, 15);

    // Helper: create an income bank transaction (positive amount)
    private BankTransaction incomeBankTx(BigDecimal amount, String description) {
        return incomeBankTx(amount, description, TEST_DATE);
    }

    private BankTransaction incomeBankTx(BigDecimal amount, String description, LocalDate date) {
        return BankTransaction.create(
            BUSINESS_ID, IMPORT_AUDIT_ID, "csv-test", date,
            amount, description, null, null,
            date + "|" + amount + "|" + description, NOW);
    }

    // Helper: create an expense bank transaction (negative amount)
    private BankTransaction expenseBankTx(BigDecimal amount, String description) {
        return expenseBankTx(amount, description, TEST_DATE);
    }

    private BankTransaction expenseBankTx(BigDecimal amount, String description, LocalDate date) {
        return BankTransaction.create(
            BUSINESS_ID, IMPORT_AUDIT_ID, "csv-test", date,
            amount.negate(), description, null, null,
            date + "|" + amount.negate() + "|" + description, NOW);
    }

    // Helper: create manual income
    private Income manualIncome(BigDecimal amount, String description) {
        return manualIncome(amount, description, TEST_DATE);
    }

    private Income manualIncome(BigDecimal amount, String description, LocalDate date) {
        return Income.create(BUSINESS_ID, date, amount, description, IncomeCategory.SALES, null);
    }

    // Helper: create manual expense
    private Expense manualExpense(BigDecimal amount, String description) {
        return manualExpense(amount, description, TEST_DATE);
    }

    private Expense manualExpense(BigDecimal amount, String description, LocalDate date) {
        return Expense.create(BUSINESS_ID, date, amount, description, ExpenseCategory.OTHER_EXPENSES, null, null);
    }

    @Nested
    @DisplayName("Empty and Null Inputs")
    class EmptyInputs {

        @Test
        void emptyBankTransactionsReturnsEmptyList() {
            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                Collections.emptyList(), List.of(), List.of(), BUSINESS_ID, NOW);
            assertThat(matches).isEmpty();
        }

        @Test
        void nullBankTransactionsReturnsEmptyList() {
            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                null, List.of(), List.of(), BUSINESS_ID, NOW);
            assertThat(matches).isEmpty();
        }

        @Test
        void nullBusinessIdThrows() {
            assertThatThrownBy(() -> ReconciliationService.reconcile(
                List.of(incomeBankTx(new BigDecimal("100"), "test")),
                List.of(), List.of(), null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void noManualEntriesReturnsEmptyList() {
            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(incomeBankTx(new BigDecimal("100"), "test")),
                Collections.emptyList(), Collections.emptyList(), BUSINESS_ID, NOW);
            assertThat(matches).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tier 1: EXACT Match")
    class ExactMatch {

        @Test
        void shouldDetectExactIncomeMatch() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("500.00"), "Client payment");
            Income income = manualIncome(new BigDecimal("500.00"), "Client payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).matchTier()).isEqualTo(MatchTier.EXACT);
            assertThat(matches.get(0).confidence()).isEqualTo(1.0);
            assertThat(matches.get(0).manualTransactionType()).isEqualTo("INCOME");
        }

        @Test
        void shouldDetectExactExpenseMatch() {
            BankTransaction bankTx = expenseBankTx(new BigDecimal("42.50"), "Office supplies");
            Expense expense = manualExpense(new BigDecimal("42.50"), "Office supplies");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(), List.of(expense), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).matchTier()).isEqualTo(MatchTier.EXACT);
            assertThat(matches.get(0).confidence()).isEqualTo(1.0);
            assertThat(matches.get(0).manualTransactionType()).isEqualTo("EXPENSE");
        }

        @Test
        void shouldMatchCaseInsensitiveDescription() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "ACME LTD PAYMENT");
            Income income = manualIncome(new BigDecimal("100.00"), "acme ltd payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).matchTier()).isEqualTo(MatchTier.EXACT);
        }

        @Test
        void shouldMatchWithWhitespaceDifferences() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "payment   for  goods");
            Income income = manualIncome(new BigDecimal("100.00"), "Payment for goods");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).matchTier()).isEqualTo(MatchTier.EXACT);
        }

        @Test
        void shouldNotMatchDifferentDates() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Payment",
                LocalDate.of(2025, 6, 15));
            Income income = manualIncome(new BigDecimal("100.00"), "Payment",
                LocalDate.of(2025, 6, 16));

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }

        @Test
        void shouldNotMatchDifferentAmounts() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Payment");
            Income income = manualIncome(new BigDecimal("100.01"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            // Not an EXACT match - should fall to POSSIBLE tier if within tolerance
            assertThat(matches).allSatisfy(m ->
                assertThat(m.matchTier()).isNotEqualTo(MatchTier.EXACT));
        }
    }

    @Nested
    @DisplayName("Tier 2: LIKELY Match")
    class LikelyMatch {

        @Test
        void shouldDetectLikelyMatchWithSimilarDescription() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("500.00"),
                "FPS ACME LTD REF 12345");
            Income income = manualIncome(new BigDecimal("500.00"),
                "FPS ACME LTD REF 12346"); // One char diff

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).matchTier()).isEqualTo(MatchTier.LIKELY);
            assertThat(matches.get(0).confidence()).isGreaterThanOrEqualTo(0.80);
        }

        @Test
        void veryDifferentDescriptionsWithExactAmountStillMatchAsPossible() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("500.00"),
                "FPS ACME LTD REF 12345");
            Income income = manualIncome(new BigDecimal("500.00"),
                "Invoice from Bob"); // Completely different description

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            // Exact amounts on the same date with very different descriptions:
            // - Not EXACT (description similarity != 1.0)
            // - Not LIKELY (description similarity < 0.80)
            // - But IS POSSIBLE (exact amounts are within tolerance)
            // This is the safety net: exact amounts on the same date always surface for review
            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).matchTier()).isEqualTo(MatchTier.POSSIBLE);
            assertThat(matches.get(0).confidence()).isEqualTo(0.30);
        }
    }

    @Nested
    @DisplayName("Tier 3: POSSIBLE Match")
    class PossibleMatch {

        @Test
        void shouldDetectPossibleMatchWithinTolerance() {
            // Bank shows 500.00, manual shows 504.00
            // 1% of 504 = 5.04, abs tolerance = 1.00
            // 5.04 > 1.00, so 1% applies. Diff = 4.00, 4.00 <= 5.04 -> within tolerance
            BankTransaction bankTx = incomeBankTx(new BigDecimal("500.00"),
                "Different description entirely");
            Income income = manualIncome(new BigDecimal("504.00"),
                "Some other payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).matchTier()).isEqualTo(MatchTier.POSSIBLE);
            assertThat(matches.get(0).confidence()).isEqualTo(0.30);
        }

        @Test
        void shouldNotMatchBeyondTolerance() {
            // Bank shows 500.00, manual shows 506.00
            // 1% of 506 = 5.06, diff = 6.00 > 5.06 -> outside tolerance
            BankTransaction bankTx = incomeBankTx(new BigDecimal("500.00"), "Payment");
            Income income = manualIncome(new BigDecimal("506.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }

        @Test
        void shouldUseAbsoluteToleranceForSmallAmounts() {
            // Bank shows 10.00, manual shows 10.90
            // 1% of 10.90 = 0.109, abs = 1.00
            // max(0.109, 1.00) = 1.00
            // Diff = 0.90 <= 1.00 -> within tolerance
            BankTransaction bankTx = expenseBankTx(new BigDecimal("10.00"), "Small purchase");
            Expense expense = manualExpense(new BigDecimal("10.90"), "Small purchase different");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(), List.of(expense), BUSINESS_ID, NOW);

            // Amount not exact, so no EXACT or LIKELY check
            // isWithinTolerance should pass
            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).matchTier()).isEqualTo(MatchTier.POSSIBLE);
        }
    }

    @Nested
    @DisplayName("Direction-Aware Matching")
    class DirectionAware {

        @Test
        void incomeBankTxShouldNotMatchExpenses() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Payment");
            Expense expense = manualExpense(new BigDecimal("100.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(), List.of(expense), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }

        @Test
        void expenseBankTxShouldNotMatchIncomes() {
            BankTransaction bankTx = expenseBankTx(new BigDecimal("100.00"), "Payment");
            Income income = manualIncome(new BigDecimal("100.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }

        @Test
        void incomeBankTxMatchesOnlyIncomes() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Payment");
            Income income = manualIncome(new BigDecimal("100.00"), "Payment");
            Expense expense = manualExpense(new BigDecimal("100.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(expense), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).manualTransactionType()).isEqualTo("INCOME");
        }

        @Test
        void expenseBankTxMatchesOnlyExpenses() {
            BankTransaction bankTx = expenseBankTx(new BigDecimal("100.00"), "Payment");
            Income income = manualIncome(new BigDecimal("100.00"), "Payment");
            Expense expense = manualExpense(new BigDecimal("100.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(expense), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).manualTransactionType()).isEqualTo("EXPENSE");
        }
    }

    @Nested
    @DisplayName("Business ID Scoping")
    class BusinessIdScoping {

        @Test
        void shouldNotMatchAcrossBusinesses() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Payment");

            // Create income for a different business
            Income otherBusinessIncome = new Income(
                UUID.randomUUID(), OTHER_BUSINESS_ID, TEST_DATE,
                new BigDecimal("100.00"), "Payment", IncomeCategory.SALES,
                null, null, null, null, null);

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(otherBusinessIncome), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }

        @Test
        void shouldSkipBankTxFromOtherBusiness() {
            // Create bank tx for a different business
            BankTransaction otherBankTx = BankTransaction.create(
                OTHER_BUSINESS_ID, IMPORT_AUDIT_ID, "csv-test", TEST_DATE,
                new BigDecimal("100.00"), "Payment", null, null,
                "hash1", NOW);

            Income income = manualIncome(new BigDecimal("100.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(otherBankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }
    }

    @Nested
    @DisplayName("Absolute Amount Comparison")
    class AbsoluteAmount {

        @Test
        void shouldUseAbsoluteAmountForExpenses() {
            // Bank tx stores expense as -42.50
            BankTransaction bankTx = expenseBankTx(new BigDecimal("42.50"), "Office supplies");
            // Manual expense stores as positive 42.50
            Expense expense = manualExpense(new BigDecimal("42.50"), "Office supplies");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(), List.of(expense), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).matchTier()).isEqualTo(MatchTier.EXACT);
        }
    }

    @Nested
    @DisplayName("Excluded Transactions")
    class ExcludedTransactions {

        @Test
        void shouldSkipExcludedIncomeBankTransaction() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Payment")
                .withExcluded("personal transfer", NOW);

            Income income = manualIncome(new BigDecimal("100.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }

        @Test
        void shouldSkipExcludedExpenseBankTransaction() {
            BankTransaction bankTx = expenseBankTx(new BigDecimal("50.00"), "Purchase")
                .withExcluded("loan repayment", NOW);

            Expense expense = manualExpense(new BigDecimal("50.00"), "Purchase");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(), List.of(expense), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }

        @Test
        void shouldMatchNonExcludedButSkipExcluded() {
            BankTransaction excluded = incomeBankTx(new BigDecimal("100.00"), "Payment")
                .withExcluded("not business", NOW);
            BankTransaction active = incomeBankTx(new BigDecimal("200.00"), "Invoice");

            Income income1 = manualIncome(new BigDecimal("100.00"), "Payment");
            Income income2 = manualIncome(new BigDecimal("200.00"), "Invoice");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(excluded, active), List.of(income1, income2), List.of(), BUSINESS_ID, NOW);

            // Only the active bank tx should produce a match
            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).bankTransactionId()).isEqualTo(active.id());
        }
    }

    @Nested
    @DisplayName("Already Linked Transactions")
    class AlreadyLinked {

        @Test
        void shouldSkipBankTxWithExistingIncomeLink() {
            // Bank tx already categorized with income link
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Payment")
                .withCategorizedAsIncome(UUID.randomUUID(), NOW);

            Income income = manualIncome(new BigDecimal("100.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }

        @Test
        void shouldSkipBankTxWithExistingExpenseLink() {
            // Bank tx already categorized with expense link
            BankTransaction bankTx = expenseBankTx(new BigDecimal("50.00"), "Purchase")
                .withCategorizedAsExpense(UUID.randomUUID(), NOW);

            Expense expense = manualExpense(new BigDecimal("50.00"), "Purchase");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(), List.of(expense), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }

        @Test
        void shouldSkipIncomeAlreadyLinkedToThisBankTx() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Payment");

            // Income that is already linked to this specific bank transaction
            Income linkedIncome = new Income(
                UUID.randomUUID(), BUSINESS_ID, TEST_DATE,
                new BigDecimal("100.00"), "Payment", IncomeCategory.SALES,
                null, null, null, null, bankTx.id());

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(linkedIncome), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }
    }

    @Nested
    @DisplayName("Zero Amount Transactions")
    class ZeroAmount {

        @Test
        void shouldSkipZeroAmountBankTransaction() {
            BankTransaction bankTx = BankTransaction.create(
                BUSINESS_ID, IMPORT_AUDIT_ID, "csv-test", TEST_DATE,
                BigDecimal.ZERO, "Zero transaction", null, null,
                "hash-zero", NOW);

            Income income = manualIncome(new BigDecimal("100.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).isEmpty();
        }
    }

    @Nested
    @DisplayName("Multiple Matches")
    class MultipleMatches {

        @Test
        void shouldFindMultipleMatchesForOneBankTx() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Client payment");

            // Two manual entries on the same date with similar amounts
            Income income1 = manualIncome(new BigDecimal("100.00"), "Client payment");
            Income income2 = manualIncome(new BigDecimal("100.00"), "Client payment different");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income1, income2), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).hasSizeGreaterThanOrEqualTo(1);
        }

        @Test
        void shouldProcessMultipleBankTransactions() {
            BankTransaction bankTx1 = incomeBankTx(new BigDecimal("100.00"), "Payment A");
            BankTransaction bankTx2 = expenseBankTx(new BigDecimal("50.00"), "Purchase B");

            Income income = manualIncome(new BigDecimal("100.00"), "Payment A");
            Expense expense = manualExpense(new BigDecimal("50.00"), "Purchase B");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx1, bankTx2), List.of(income), List.of(expense), BUSINESS_ID, NOW);

            assertThat(matches).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Match Status")
    class MatchStatus {

        @Test
        void allDetectedMatchesAreUnresolved() {
            BankTransaction bankTx = incomeBankTx(new BigDecimal("100.00"), "Payment");
            Income income = manualIncome(new BigDecimal("100.00"), "Payment");

            List<ReconciliationMatch> matches = ReconciliationService.reconcile(
                List.of(bankTx), List.of(income), List.of(), BUSINESS_ID, NOW);

            assertThat(matches).allSatisfy(m -> {
                assertThat(m.status()).isEqualTo(ReconciliationStatus.UNRESOLVED);
                assertThat(m.resolvedAt()).isNull();
                assertThat(m.resolvedBy()).isNull();
            });
        }
    }
}
