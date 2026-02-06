package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SqliteIncomeRepository.
 * Tests income persistence using in-memory SQLite database for isolation.
 */
@DisplayName("SqliteIncomeRepository")
class SqliteIncomeRepositoryTest {

    private SqliteIncomeRepository repository;
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
        repository = new SqliteIncomeRepository(testBusinessId);
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.resetTestData();
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("should save income and return it")
        void shouldSaveIncomeAndReturnIt() {
            Income income = createTestIncome();

            Income saved = repository.save(income);

            assertThat(saved).isNotNull();
            assertThat(saved.id()).isEqualTo(income.id());
        }

        @Test
        @DisplayName("should persist income to database")
        void shouldPersistIncomeToDatabase() {
            Income income = createTestIncome();

            repository.save(income);

            // Create new repository instance to verify persistence
            SqliteIncomeRepository newRepo = new SqliteIncomeRepository(testBusinessId);
            Optional<Income> found = newRepo.findById(income.id());
            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("should update existing income")
        void shouldUpdateExistingIncome() {
            Income original = createTestIncome();
            repository.save(original);

            Income updated = new Income(
                    original.id(),
                    original.businessId(),
                    original.date(),
                    new BigDecimal("5000.00"),
                    "Updated invoice",
                    IncomeCategory.OTHER_INCOME,
                    "INV-UPDATED",
                    null,
                    null,
                    null,
                    null
            );
            repository.save(updated);

            Optional<Income> found = repository.findById(original.id());
            assertThat(found).isPresent();
            assertThat(found.get().amount()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(found.get().description()).isEqualTo("Updated invoice");
            assertThat(found.get().category()).isEqualTo(IncomeCategory.OTHER_INCOME);
        }
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("should find income by ID")
        void shouldFindIncomeById() {
            Income income = createTestIncome();
            repository.save(income);

            Optional<Income> found = repository.findById(income.id());

            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(income.id());
            assertThat(found.get().businessId()).isEqualTo(income.businessId());
            assertThat(found.get().amount()).isEqualByComparingTo(income.amount());
        }

        @Test
        @DisplayName("should return empty when income not found")
        void shouldReturnEmptyWhenIncomeNotFound() {
            Optional<Income> found = repository.findById(UUID.randomUUID());

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
        @DisplayName("should find all income for business")
        void shouldFindAllIncomeForBusiness() {
            repository.save(createTestIncome());
            repository.save(createTestIncome("Second invoice", new BigDecimal("2000.00")));

            List<Income> incomeList = repository.findAll();

            assertThat(incomeList).hasSize(2);
        }

        @Test
        @DisplayName("should return income sorted by date descending")
        void shouldReturnIncomeSortedByDateDescending() {
            Income older = createTestIncomeWithDate(LocalDate.of(2024, 1, 1));
            Income newer = createTestIncomeWithDate(LocalDate.of(2024, 6, 1));
            repository.save(older);
            repository.save(newer);

            List<Income> incomeList = repository.findAll();

            assertThat(incomeList).hasSize(2);
            assertThat(incomeList.get(0).date()).isAfter(incomeList.get(1).date());
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
        @DisplayName("should find income by tax year")
        void shouldFindIncomeByTaxYear() {
            Income inRange = createTestIncomeWithDate(LocalDate.of(2024, 6, 15));
            Income beforeRange = createTestIncomeWithDate(LocalDate.of(2024, 3, 1));
            Income afterRange = createTestIncomeWithDate(LocalDate.of(2025, 5, 1));
            repository.save(inRange);
            repository.save(beforeRange);
            repository.save(afterRange);

            List<Income> found = repository.findByTaxYear(taxYear2024);

            assertThat(found).hasSize(1);
            assertThat(found.get(0).id()).isEqualTo(inRange.id());
        }

        @Test
        @DisplayName("should include income on tax year boundaries")
        void shouldIncludeIncomeOnTaxYearBoundaries() {
            Income onStartDate = createTestIncomeWithDate(LocalDate.of(2024, 4, 6));
            Income onEndDate = createTestIncomeWithDate(LocalDate.of(2025, 4, 5));
            repository.save(onStartDate);
            repository.save(onEndDate);

            List<Income> found = repository.findByTaxYear(taxYear2024);

            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("should calculate total for tax year")
        void shouldCalculateTotalForTaxYear() {
            repository.save(createTestIncomeWithDateAndAmount(LocalDate.of(2024, 5, 1), new BigDecimal("1000.00")));
            repository.save(createTestIncomeWithDateAndAmount(LocalDate.of(2024, 6, 1), new BigDecimal("2500.50")));
            repository.save(createTestIncomeWithDateAndAmount(LocalDate.of(2023, 1, 1), new BigDecimal("9999.00"))); // Out of range

            BigDecimal total = repository.getTotalByTaxYear(taxYear2024);

            assertThat(total).isEqualByComparingTo(new BigDecimal("3500.50"));
        }

        @Test
        @DisplayName("should return zero total when no income in tax year")
        void shouldReturnZeroTotalWhenNoIncomeInTaxYear() {
            BigDecimal total = repository.getTotalByTaxYear(taxYear2024);

            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Category Queries")
    class CategoryQueries {

        @Test
        @DisplayName("should find income by category")
        void shouldFindIncomeByCategory() {
            repository.save(createIncomeWithCategory(IncomeCategory.SALES, new BigDecimal("1000.00")));
            repository.save(createIncomeWithCategory(IncomeCategory.OTHER_INCOME, new BigDecimal("200.00")));
            repository.save(createIncomeWithCategory(IncomeCategory.SALES, new BigDecimal("1500.00")));

            List<Income> found = repository.findByCategory(IncomeCategory.SALES);

            assertThat(found).hasSize(2);
            assertThat(found).allMatch(i -> i.category() == IncomeCategory.SALES);
        }

        @Test
        @DisplayName("should throw when finding by null category")
        void shouldThrowWhenFindingByNullCategory() {
            assertThatThrownBy(() -> repository.findByCategory(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("should delete income")
        void shouldDeleteIncome() {
            Income income = createTestIncome();
            repository.save(income);

            boolean deleted = repository.delete(income.id());

            assertThat(deleted).isTrue();
            assertThat(repository.findById(income.id())).isEmpty();
        }

        @Test
        @DisplayName("should return false when deleting non-existent income")
        void shouldReturnFalseWhenDeletingNonExistentIncome() {
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
            repository.save(createTestIncome());
            repository.save(createTestIncome("Second", new BigDecimal("2000.00")));

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

    private Income createTestIncome() {
        return createTestIncome("Test invoice", new BigDecimal("1000.00"));
    }

    private Income createTestIncome(String description, BigDecimal amount) {
        return new Income(
                UUID.randomUUID(),
                testBusinessId,
                LocalDate.now().minusDays(1),
                amount,
                description,
                IncomeCategory.SALES,
                "REF-001",
                null,
                null,
                null,
                null
        );
    }

    private Income createTestIncomeWithDate(LocalDate date) {
        return new Income(
                UUID.randomUUID(),
                testBusinessId,
                date,
                new BigDecimal("1000.00"),
                "Test invoice",
                IncomeCategory.SALES,
                "REF-001",
                null,
                null,
                null,
                null
        );
    }

    private Income createTestIncomeWithDateAndAmount(LocalDate date, BigDecimal amount) {
        return new Income(
                UUID.randomUUID(),
                testBusinessId,
                date,
                amount,
                "Test invoice",
                IncomeCategory.SALES,
                "REF-001",
                null,
                null,
                null,
                null
        );
    }

    private Income createIncomeWithCategory(IncomeCategory category, BigDecimal amount) {
        return new Income(
                UUID.randomUUID(),
                testBusinessId,
                LocalDate.of(2024, 6, 15),
                amount,
                "Test income - " + category,
                category,
                "REF-001",
                null,
                null,
                null,
                null
        );
    }
}
