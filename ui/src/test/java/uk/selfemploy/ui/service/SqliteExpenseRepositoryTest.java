package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SqliteExpenseRepository.
 * Tests expense persistence using in-memory SQLite database for isolation.
 */
@DisplayName("SqliteExpenseRepository")
class SqliteExpenseRepositoryTest {

    private SqliteExpenseRepository repository;
    private UUID testBusinessId;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetInstance();
        testBusinessId = UUID.randomUUID();
        repository = new SqliteExpenseRepository(testBusinessId);
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.resetTestData();
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("should save expense and return it")
        void shouldSaveExpenseAndReturnIt() {
            Expense expense = createTestExpense();

            Expense saved = repository.save(expense);

            assertThat(saved).isNotNull();
            assertThat(saved.id()).isEqualTo(expense.id());
        }

        @Test
        @DisplayName("should persist expense to database")
        void shouldPersistExpenseToDatabase() {
            Expense expense = createTestExpense();

            repository.save(expense);

            // Create new repository instance to verify persistence
            SqliteExpenseRepository newRepo = new SqliteExpenseRepository(testBusinessId);
            Optional<Expense> found = newRepo.findById(expense.id());
            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("should update existing expense")
        void shouldUpdateExistingExpense() {
            Expense original = createTestExpense();
            repository.save(original);

            Expense updated = new Expense(
                    original.id(),
                    original.businessId(),
                    original.date(),
                    new BigDecimal("999.99"),
                    "Updated description",
                    ExpenseCategory.TRAVEL,
                    "/path/to/receipt.pdf",
                    "Updated notes",
                    null,
                    null,
                    null,
                    null
            );
            repository.save(updated);

            Optional<Expense> found = repository.findById(original.id());
            assertThat(found).isPresent();
            assertThat(found.get().amount()).isEqualByComparingTo(new BigDecimal("999.99"));
            assertThat(found.get().description()).isEqualTo("Updated description");
            assertThat(found.get().category()).isEqualTo(ExpenseCategory.TRAVEL);
        }
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("should find expense by ID")
        void shouldFindExpenseById() {
            Expense expense = createTestExpense();
            repository.save(expense);

            Optional<Expense> found = repository.findById(expense.id());

            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(expense.id());
            assertThat(found.get().businessId()).isEqualTo(expense.businessId());
            assertThat(found.get().amount()).isEqualByComparingTo(expense.amount());
        }

        @Test
        @DisplayName("should return empty when expense not found")
        void shouldReturnEmptyWhenExpenseNotFound() {
            Optional<Expense> found = repository.findById(UUID.randomUUID());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should throw when finding with null ID")
        void shouldThrowWhenFindingWithNullId() {
            assertThatThrownBy(() -> repository.findById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("should find all expenses for business")
        void shouldFindAllExpensesForBusiness() {
            repository.save(createTestExpense());
            repository.save(createTestExpense("Second expense", new BigDecimal("200.00")));

            List<Expense> expenses = repository.findAll();

            assertThat(expenses).hasSize(2);
        }

        @Test
        @DisplayName("should return expenses sorted by date descending")
        void shouldReturnExpensesSortedByDateDescending() {
            Expense older = createTestExpenseWithDate(LocalDate.of(2024, 1, 1));
            Expense newer = createTestExpenseWithDate(LocalDate.of(2024, 6, 1));
            repository.save(older);
            repository.save(newer);

            List<Expense> expenses = repository.findAll();

            assertThat(expenses).hasSize(2);
            assertThat(expenses.get(0).date()).isAfter(expenses.get(1).date());
        }
    }

    @Nested
    @DisplayName("Tax Year Queries")
    class TaxYearQueries {

        private TaxYear taxYear2024;

        @BeforeEach
        void setUp() {
            taxYear2024 = TaxYear.of(2024); // April 6, 2024 to April 5, 2025
        }

        @Test
        @DisplayName("should find expenses by tax year")
        void shouldFindExpensesByTaxYear() {
            Expense inRange = createTestExpenseWithDate(LocalDate.of(2024, 6, 15));
            Expense beforeRange = createTestExpenseWithDate(LocalDate.of(2024, 3, 1));
            Expense afterRange = createTestExpenseWithDate(LocalDate.of(2025, 5, 1));
            repository.save(inRange);
            repository.save(beforeRange);
            repository.save(afterRange);

            List<Expense> found = repository.findByTaxYear(taxYear2024);

            assertThat(found).hasSize(1);
            assertThat(found.get(0).id()).isEqualTo(inRange.id());
        }

        @Test
        @DisplayName("should include expenses on tax year boundaries")
        void shouldIncludeExpensesOnTaxYearBoundaries() {
            Expense onStartDate = createTestExpenseWithDate(LocalDate.of(2024, 4, 6));
            Expense onEndDate = createTestExpenseWithDate(LocalDate.of(2025, 4, 5));
            repository.save(onStartDate);
            repository.save(onEndDate);

            List<Expense> found = repository.findByTaxYear(taxYear2024);

            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("should calculate total for tax year")
        void shouldCalculateTotalForTaxYear() {
            repository.save(createTestExpenseWithDateAndAmount(LocalDate.of(2024, 5, 1), new BigDecimal("100.00")));
            repository.save(createTestExpenseWithDateAndAmount(LocalDate.of(2024, 6, 1), new BigDecimal("200.50")));
            repository.save(createTestExpenseWithDateAndAmount(LocalDate.of(2023, 1, 1), new BigDecimal("999.00"))); // Out of range

            BigDecimal total = repository.getTotalByTaxYear(taxYear2024);

            assertThat(total).isEqualByComparingTo(new BigDecimal("300.50"));
        }

        @Test
        @DisplayName("should return zero total when no expenses in tax year")
        void shouldReturnZeroTotalWhenNoExpensesInTaxYear() {
            BigDecimal total = repository.getTotalByTaxYear(taxYear2024);

            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Allowable Expenses")
    class AllowableExpenses {

        private TaxYear taxYear2024;

        @BeforeEach
        void setUp() {
            taxYear2024 = TaxYear.of(2024);
        }

        @Test
        @DisplayName("should calculate allowable total")
        void shouldCalculateAllowableTotal() {
            // Allowable expense
            Expense allowable = createExpenseWithCategory(ExpenseCategory.OFFICE_COSTS, new BigDecimal("100.00"));
            // Non-allowable expense
            Expense nonAllowable = createExpenseWithCategory(ExpenseCategory.BUSINESS_ENTERTAINMENT, new BigDecimal("50.00"));

            repository.save(allowable);
            repository.save(nonAllowable);

            BigDecimal allowableTotal = repository.getAllowableTotalByTaxYear(taxYear2024);

            assertThat(allowableTotal).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should return zero allowable when all non-allowable")
        void shouldReturnZeroAllowableWhenAllNonAllowable() {
            repository.save(createExpenseWithCategory(ExpenseCategory.BUSINESS_ENTERTAINMENT, new BigDecimal("100.00")));
            repository.save(createExpenseWithCategory(ExpenseCategory.DEPRECIATION, new BigDecimal("200.00")));

            BigDecimal allowableTotal = repository.getAllowableTotalByTaxYear(taxYear2024);

            assertThat(allowableTotal).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Category Queries")
    class CategoryQueries {

        @Test
        @DisplayName("should find expenses by category")
        void shouldFindExpensesByCategory() {
            repository.save(createExpenseWithCategory(ExpenseCategory.OFFICE_COSTS, new BigDecimal("100.00")));
            repository.save(createExpenseWithCategory(ExpenseCategory.TRAVEL, new BigDecimal("200.00")));
            repository.save(createExpenseWithCategory(ExpenseCategory.OFFICE_COSTS, new BigDecimal("150.00")));

            List<Expense> found = repository.findByCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(found).hasSize(2);
            assertThat(found).allMatch(e -> e.category() == ExpenseCategory.OFFICE_COSTS);
        }

        @Test
        @DisplayName("should calculate totals by category for tax year")
        void shouldCalculateTotalsByCategoryForTaxYear() {
            TaxYear taxYear = TaxYear.of(2024);
            repository.save(createExpenseWithCategoryAndDate(ExpenseCategory.OFFICE_COSTS, new BigDecimal("100.00"), LocalDate.of(2024, 5, 1)));
            repository.save(createExpenseWithCategoryAndDate(ExpenseCategory.OFFICE_COSTS, new BigDecimal("50.00"), LocalDate.of(2024, 6, 1)));
            repository.save(createExpenseWithCategoryAndDate(ExpenseCategory.TRAVEL, new BigDecimal("200.00"), LocalDate.of(2024, 7, 1)));

            Map<ExpenseCategory, BigDecimal> totals = repository.getTotalsByCategoryForTaxYear(taxYear);

            assertThat(totals).containsEntry(ExpenseCategory.OFFICE_COSTS, new BigDecimal("150.00"));
            assertThat(totals).containsEntry(ExpenseCategory.TRAVEL, new BigDecimal("200.00"));
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("should delete expense")
        void shouldDeleteExpense() {
            Expense expense = createTestExpense();
            repository.save(expense);

            boolean deleted = repository.delete(expense.id());

            assertThat(deleted).isTrue();
            assertThat(repository.findById(expense.id())).isEmpty();
        }

        @Test
        @DisplayName("should return false when deleting non-existent expense")
        void shouldReturnFalseWhenDeletingNonExistentExpense() {
            boolean deleted = repository.delete(UUID.randomUUID());

            assertThat(deleted).isFalse();
        }

        @Test
        @DisplayName("should throw when deleting with null ID")
        void shouldThrowWhenDeletingWithNullId() {
            assertThatThrownBy(() -> repository.delete(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }
    }

    @Nested
    @DisplayName("Count Operations")
    class CountOperations {

        @Test
        @DisplayName("should return correct count")
        void shouldReturnCorrectCount() {
            repository.save(createTestExpense());
            repository.save(createTestExpense("Second", new BigDecimal("200.00")));

            long count = repository.count();

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero when empty")
        void shouldReturnZeroWhenEmpty() {
            long count = repository.count();

            assertThat(count).isZero();
        }
    }

    // === Test Helper Methods ===

    private Expense createTestExpense() {
        return createTestExpense("Test expense", new BigDecimal("100.00"));
    }

    private Expense createTestExpense(String description, BigDecimal amount) {
        return new Expense(
                UUID.randomUUID(),
                testBusinessId,
                LocalDate.now().minusDays(1),
                amount,
                description,
                ExpenseCategory.OFFICE_COSTS,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Expense createTestExpenseWithDate(LocalDate date) {
        return new Expense(
                UUID.randomUUID(),
                testBusinessId,
                date,
                new BigDecimal("100.00"),
                "Test expense",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Expense createTestExpenseWithDateAndAmount(LocalDate date, BigDecimal amount) {
        return new Expense(
                UUID.randomUUID(),
                testBusinessId,
                date,
                amount,
                "Test expense",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Expense createExpenseWithCategory(ExpenseCategory category, BigDecimal amount) {
        return new Expense(
                UUID.randomUUID(),
                testBusinessId,
                LocalDate.of(2024, 6, 15),
                amount,
                "Test expense - " + category,
                category,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Expense createExpenseWithCategoryAndDate(ExpenseCategory category, BigDecimal amount, LocalDate date) {
        return new Expense(
                UUID.randomUUID(),
                testBusinessId,
                date,
                amount,
                "Test expense - " + category,
                category,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
