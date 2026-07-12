package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionReviewCommitService")
class TransactionReviewCommitServiceTest {

    private static UUID businessId;
    private SqliteBankTransactionService bankTransactionService;
    private SqliteIncomeService incomeService;
    private SqliteExpenseService expenseService;
    private TransactionReviewCommitService commitService;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
        businessId = UUID.randomUUID();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
        bankTransactionService = new SqliteBankTransactionService(businessId);
        incomeService = new SqliteIncomeService(businessId);
        expenseService = new SqliteExpenseService(businessId);
        commitService = new TransactionReviewCommitService(
            bankTransactionService, incomeService, expenseService, businessId);
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.resetTestData();
    }

    private BankTransaction staged(BigDecimal signedAmount, ExpenseCategory suggested) {
        BankTransaction tx = BankTransaction.create(
            businessId, UUID.randomUUID(), null, LocalDate.of(2025, 6, 1), signedAmount,
            "Transaction", null, null, "hash-" + UUID.randomUUID(), Instant.now());
        if (suggested != null) {
            tx = tx.withSuggestion(suggested, null, Instant.now());
        }
        bankTransactionService.save(tx);
        return tx;
    }

    @Test
    @DisplayName("commits a positive transaction as an income record and links it")
    void commitsIncome() {
        BankTransaction tx = staged(new BigDecimal("250.00"), null);

        UUID incomeId = commitService.commitAsBusiness(tx, Instant.now());

        Optional<Income> income = incomeService.findById(incomeId);
        assertThat(income).isPresent();
        assertThat(income.get().amount()).isEqualByComparingTo("250.00");

        BankTransaction updated = bankTransactionService.findById(tx.id()).orElseThrow();
        assertThat(updated.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
        assertThat(updated.incomeId()).isEqualTo(incomeId);
    }

    @Test
    @DisplayName("commits a negative transaction as an expense record using the suggested category")
    void commitsExpenseWithSuggestedCategory() {
        BankTransaction tx = staged(new BigDecimal("-89.99"), ExpenseCategory.OFFICE_COSTS);

        UUID expenseId = commitService.commitAsBusiness(tx, Instant.now());

        Optional<Expense> expense = expenseService.findById(expenseId);
        assertThat(expense).isPresent();
        assertThat(expense.get().amount()).isEqualByComparingTo("89.99"); // absolute
        assertThat(expense.get().category()).isEqualTo(ExpenseCategory.OFFICE_COSTS);

        BankTransaction updated = bankTransactionService.findById(tx.id()).orElseThrow();
        assertThat(updated.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
        assertThat(updated.expenseId()).isEqualTo(expenseId);
    }

    @Test
    @DisplayName("commits an uncategorised expense using the default Other expenses category")
    void commitsExpenseWithDefaultCategory() {
        BankTransaction tx = staged(new BigDecimal("-10.00"), null);

        UUID expenseId = commitService.commitAsBusiness(tx, Instant.now());

        assertThat(expenseService.findById(expenseId).orElseThrow().category())
            .isEqualTo(ExpenseCategory.OTHER_EXPENSES);
    }

    @Test
    @DisplayName("revertCommit deletes the created income record")
    void revertDeletesCreatedRecord() {
        BankTransaction tx = staged(new BigDecimal("100.00"), null);
        UUID incomeId = commitService.commitAsBusiness(tx, Instant.now());
        assertThat(incomeService.findById(incomeId)).isPresent();

        BankTransaction committed = bankTransactionService.findById(tx.id()).orElseThrow();
        commitService.revertCommit(committed);

        assertThat(incomeService.findById(incomeId)).isEmpty();
    }
}
