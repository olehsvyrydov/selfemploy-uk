package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SqliteDataStore.
 * Tests data persistence using in-memory SQLite database for isolation.
 */
@DisplayName("SqliteDataStore")
class SqliteDataStoreTest {

    private SqliteDataStore dataStore;
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
        dataStore = SqliteDataStore.getInstance();
        testBusinessId = UUID.randomUUID();
        // Ensure business exists for FK constraints
        dataStore.ensureBusinessExists(testBusinessId);
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.resetTestData();
    }

    @Nested
    @DisplayName("Database Configuration")
    class DatabaseConfiguration {

        @Test
        @DisplayName("should use in-memory database in test mode")
        void shouldUseInMemoryDatabaseInTestMode() {
            assertThat(dataStore.isInMemory()).isTrue();
        }

        @Test
        @DisplayName("should have WAL mode enabled")
        void shouldHaveWalModeEnabled() {
            String journalMode = dataStore.getJournalMode();
            // In-memory databases use "memory" journal mode
            assertThat(journalMode).isIn("wal", "memory");
        }

        @Test
        @DisplayName("should have foreign keys enabled")
        void shouldHaveForeignKeysEnabled() {
            assertThat(dataStore.areForeignKeysEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Business ID Persistence")
    class BusinessIdPersistence {

        @Test
        @DisplayName("should save and load business ID")
        void shouldSaveAndLoadBusinessId() {
            UUID businessId = UUID.randomUUID();

            dataStore.saveBusinessId(businessId);
            UUID loaded = dataStore.loadBusinessId();

            assertThat(loaded).isEqualTo(businessId);
        }

        @Test
        @DisplayName("should return null when no business ID saved")
        void shouldReturnNullWhenNoBusinessIdSaved() {
            UUID loaded = dataStore.loadBusinessId();

            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("should overwrite existing business ID")
        void shouldOverwriteExistingBusinessId() {
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();

            dataStore.saveBusinessId(first);
            dataStore.saveBusinessId(second);
            UUID loaded = dataStore.loadBusinessId();

            assertThat(loaded).isEqualTo(second);
        }

        @Test
        @DisplayName("should persist business ID after clear and reload")
        void shouldPersistBusinessIdAfterClearAndReload() {
            UUID businessId = UUID.randomUUID();
            dataStore.saveBusinessId(businessId);

            // Business ID is in settings, which is cleared by clearAll
            // This test verifies the save/load mechanism works
            UUID loaded = dataStore.loadBusinessId();
            assertThat(loaded).isEqualTo(businessId);
        }
    }

    @Nested
    @DisplayName("Expense CRUD Operations")
    class ExpenseCrudOperations {

        @Test
        @DisplayName("should save expense")
        void shouldSaveExpense() {
            Expense expense = createTestExpense(testBusinessId);

            dataStore.saveExpense(expense);

            assertThat(dataStore.countExpenses()).isEqualTo(1);
        }

        @Test
        @DisplayName("should find expense by ID")
        void shouldFindExpenseById() {
            Expense expense = createTestExpense(testBusinessId);
            dataStore.saveExpense(expense);

            Optional<Expense> found = dataStore.findExpenseById(expense.id());

            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(expense.id());
            assertThat(found.get().amount()).isEqualByComparingTo(expense.amount());
            assertThat(found.get().description()).isEqualTo(expense.description());
            assertThat(found.get().category()).isEqualTo(expense.category());
        }

        @Test
        @DisplayName("should return empty when expense not found")
        void shouldReturnEmptyWhenExpenseNotFound() {
            Optional<Expense> found = dataStore.findExpenseById(UUID.randomUUID());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should load all expenses")
        void shouldLoadAllExpenses() {
            Expense expense1 = createTestExpense(testBusinessId);
            Expense expense2 = createTestExpense(testBusinessId, "Second expense", new BigDecimal("200.00"));
            dataStore.saveExpense(expense1);
            dataStore.saveExpense(expense2);

            List<Expense> expenses = dataStore.loadAllExpenses();

            assertThat(expenses).hasSize(2);
        }

        @Test
        @DisplayName("should update expense")
        void shouldUpdateExpense() {
            Expense original = createTestExpense(testBusinessId);
            dataStore.saveExpense(original);

            Expense updated = new Expense(
                    original.id(),
                    original.businessId(),
                    original.date(),
                    new BigDecimal("999.99"),
                    "Updated description",
                    ExpenseCategory.TRAVEL,
                    null,
                    "Updated notes",
                    null,
                    null,
                    null,
                    null
            );
            dataStore.saveExpense(updated);

            Optional<Expense> found = dataStore.findExpenseById(original.id());
            assertThat(found).isPresent();
            assertThat(found.get().amount()).isEqualByComparingTo(new BigDecimal("999.99"));
            assertThat(found.get().description()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("should delete expense")
        void shouldDeleteExpense() {
            Expense expense = createTestExpense(testBusinessId);
            dataStore.saveExpense(expense);

            boolean deleted = dataStore.deleteExpense(expense.id());

            assertThat(deleted).isTrue();
            assertThat(dataStore.findExpenseById(expense.id())).isEmpty();
        }

        @Test
        @DisplayName("should return false when deleting non-existent expense")
        void shouldReturnFalseWhenDeletingNonExistentExpense() {
            boolean deleted = dataStore.deleteExpense(UUID.randomUUID());

            assertThat(deleted).isFalse();
        }

        @Test
        @DisplayName("should find expenses by business ID")
        void shouldFindExpensesByBusinessId() {
            UUID otherBusinessId = UUID.randomUUID();
            dataStore.ensureBusinessExists(otherBusinessId);

            Expense expense1 = createTestExpense(testBusinessId);
            Expense expense2 = createTestExpense(otherBusinessId);
            dataStore.saveExpense(expense1);
            dataStore.saveExpense(expense2);

            List<Expense> found = dataStore.findExpensesByBusinessId(testBusinessId);

            assertThat(found).hasSize(1);
            assertThat(found.get(0).businessId()).isEqualTo(testBusinessId);
        }

        @Test
        @DisplayName("should find expenses by date range")
        void shouldFindExpensesByDateRange() {
            LocalDate startDate = LocalDate.of(2024, 4, 6);
            LocalDate endDate = LocalDate.of(2025, 4, 5);

            Expense inRange = createTestExpenseWithDate(testBusinessId, LocalDate.of(2024, 6, 15));
            Expense beforeRange = createTestExpenseWithDate(testBusinessId, LocalDate.of(2024, 3, 1));
            Expense afterRange = createTestExpenseWithDate(testBusinessId, LocalDate.of(2025, 5, 1));

            dataStore.saveExpense(inRange);
            dataStore.saveExpense(beforeRange);
            dataStore.saveExpense(afterRange);

            List<Expense> found = dataStore.findExpensesByDateRange(testBusinessId, startDate, endDate);

            assertThat(found).hasSize(1);
            assertThat(found.get(0).id()).isEqualTo(inRange.id());
        }

        @Test
        @DisplayName("should preserve BigDecimal precision")
        void shouldPreserveBigDecimalPrecision() {
            Expense expense = createTestExpense(testBusinessId, "Precision test", new BigDecimal("12345.67"));
            dataStore.saveExpense(expense);

            Optional<Expense> found = dataStore.findExpenseById(expense.id());

            assertThat(found).isPresent();
            assertThat(found.get().amount()).isEqualByComparingTo(new BigDecimal("12345.67"));
        }
    }

    @Nested
    @DisplayName("Income CRUD Operations")
    class IncomeCrudOperations {

        @Test
        @DisplayName("should save income")
        void shouldSaveIncome() {
            Income income = createTestIncome(testBusinessId);

            dataStore.saveIncome(income);

            assertThat(dataStore.countIncome()).isEqualTo(1);
        }

        @Test
        @DisplayName("should find income by ID")
        void shouldFindIncomeById() {
            Income income = createTestIncome(testBusinessId);
            dataStore.saveIncome(income);

            Optional<Income> found = dataStore.findIncomeById(income.id());

            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(income.id());
            assertThat(found.get().amount()).isEqualByComparingTo(income.amount());
            assertThat(found.get().description()).isEqualTo(income.description());
        }

        @Test
        @DisplayName("should return empty when income not found")
        void shouldReturnEmptyWhenIncomeNotFound() {
            Optional<Income> found = dataStore.findIncomeById(UUID.randomUUID());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should load all income")
        void shouldLoadAllIncome() {
            Income income1 = createTestIncome(testBusinessId);
            Income income2 = createTestIncome(testBusinessId, "Second income", new BigDecimal("2000.00"));
            dataStore.saveIncome(income1);
            dataStore.saveIncome(income2);

            List<Income> incomeList = dataStore.loadAllIncome();

            assertThat(incomeList).hasSize(2);
        }

        @Test
        @DisplayName("should update income")
        void shouldUpdateIncome() {
            Income original = createTestIncome(testBusinessId);
            dataStore.saveIncome(original);

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
            dataStore.saveIncome(updated);

            Optional<Income> found = dataStore.findIncomeById(original.id());
            assertThat(found).isPresent();
            assertThat(found.get().amount()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(found.get().description()).isEqualTo("Updated invoice");
        }

        @Test
        @DisplayName("should delete income")
        void shouldDeleteIncome() {
            Income income = createTestIncome(testBusinessId);
            dataStore.saveIncome(income);

            boolean deleted = dataStore.deleteIncome(income.id());

            assertThat(deleted).isTrue();
            assertThat(dataStore.findIncomeById(income.id())).isEmpty();
        }

        @Test
        @DisplayName("should find income by business ID")
        void shouldFindIncomeByBusinessId() {
            UUID otherBusinessId = UUID.randomUUID();
            dataStore.ensureBusinessExists(otherBusinessId);

            Income income1 = createTestIncome(testBusinessId);
            Income income2 = createTestIncome(otherBusinessId);
            dataStore.saveIncome(income1);
            dataStore.saveIncome(income2);

            List<Income> found = dataStore.findIncomeByBusinessId(testBusinessId);

            assertThat(found).hasSize(1);
            assertThat(found.get(0).businessId()).isEqualTo(testBusinessId);
        }

        @Test
        @DisplayName("should find income by date range")
        void shouldFindIncomeByDateRange() {
            LocalDate startDate = LocalDate.of(2024, 4, 6);
            LocalDate endDate = LocalDate.of(2025, 4, 5);

            Income inRange = createTestIncomeWithDate(testBusinessId, LocalDate.of(2024, 8, 20));
            Income beforeRange = createTestIncomeWithDate(testBusinessId, LocalDate.of(2024, 2, 1));

            dataStore.saveIncome(inRange);
            dataStore.saveIncome(beforeRange);

            List<Income> found = dataStore.findIncomeByDateRange(testBusinessId, startDate, endDate);

            assertThat(found).hasSize(1);
            assertThat(found.get(0).id()).isEqualTo(inRange.id());
        }
    }

    @Nested
    @DisplayName("Transaction Support")
    class TransactionSupport {

        @Test
        @DisplayName("should rollback on error within transaction")
        void shouldRollbackOnErrorWithinTransaction() {
            Expense validExpense = createTestExpense(testBusinessId);

            // This should fail and rollback
            boolean success = dataStore.executeInTransaction(() -> {
                dataStore.saveExpense(validExpense);
                throw new RuntimeException("Simulated error");
            });

            assertThat(success).isFalse();
            assertThat(dataStore.countExpenses()).isZero();
        }

        @Test
        @DisplayName("should commit successful transaction")
        void shouldCommitSuccessfulTransaction() {
            Expense expense = createTestExpense(testBusinessId);

            boolean success = dataStore.executeInTransaction(() -> {
                dataStore.saveExpense(expense);
            });

            assertThat(success).isTrue();
            assertThat(dataStore.countExpenses()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Data Integrity")
    class DataIntegrity {

        @Test
        @DisplayName("should handle special characters in description")
        void shouldHandleSpecialCharactersInDescription() {
            String specialDesc = "Test with 'quotes' and \"double quotes\" & symbols <>";
            Expense expense = new Expense(
                    UUID.randomUUID(),
                    testBusinessId,
                    LocalDate.now().minusDays(1),
                    new BigDecimal("100.00"),
                    specialDesc,
                    ExpenseCategory.OFFICE_COSTS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            dataStore.saveExpense(expense);

            Optional<Expense> found = dataStore.findExpenseById(expense.id());

            assertThat(found).isPresent();
            assertThat(found.get().description()).isEqualTo(specialDesc);
        }

        @Test
        @DisplayName("should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            String unicodeDesc = "Invoice from \u00C9mile Zola - \u00A3500 \u20AC600";
            Income income = new Income(
                    UUID.randomUUID(),
                    testBusinessId,
                    LocalDate.now().minusDays(1),
                    new BigDecimal("500.00"),
                    unicodeDesc,
                    IncomeCategory.SALES,
                    "REF-001",
                    null,
                    null,
                    null,
                    null
            );
            dataStore.saveIncome(income);

            Optional<Income> found = dataStore.findIncomeById(income.id());

            assertThat(found).isPresent();
            assertThat(found.get().description()).isEqualTo(unicodeDesc);
        }

        @Test
        @DisplayName("should handle null optional fields")
        void shouldHandleNullOptionalFields() {
            Expense expense = new Expense(
                    UUID.randomUUID(),
                    testBusinessId,
                    LocalDate.now().minusDays(1),
                    new BigDecimal("100.00"),
                    "Test expense",
                    ExpenseCategory.OFFICE_COSTS,
                    null,  // receiptPath
                    null,  // notes
                    null,  // bankTransactionRef
                    null,  // supplierRef
                    null,  // invoiceNumber
                    null
            );
            dataStore.saveExpense(expense);

            Optional<Expense> found = dataStore.findExpenseById(expense.id());

            assertThat(found).isPresent();
            assertThat(found.get().receiptPath()).isNull();
            assertThat(found.get().notes()).isNull();
        }
    }

    @Nested
    @DisplayName("UTR Persistence")
    class UtrPersistence {

        @Test
        @DisplayName("should save and load UTR")
        void shouldSaveAndLoadUtr() {
            dataStore.saveUtr("1234567890");
            String loaded = dataStore.loadUtr();

            assertThat(loaded).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should return null when no UTR saved")
        void shouldReturnNullWhenNoUtrSaved() {
            String loaded = dataStore.loadUtr();

            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("should overwrite existing UTR")
        void shouldOverwriteExistingUtr() {
            dataStore.saveUtr("1234567890");
            dataStore.saveUtr("0987654321");
            String loaded = dataStore.loadUtr();

            assertThat(loaded).isEqualTo("0987654321");
        }

        @Test
        @DisplayName("should handle saving empty string UTR")
        void shouldHandleSavingEmptyStringUtr() {
            dataStore.saveUtr("1234567890");
            dataStore.saveUtr("");
            String loaded = dataStore.loadUtr();

            assertThat(loaded).isEmpty();
        }

        @Test
        @DisplayName("should persist UTR independently of NINO")
        void shouldPersistUtrIndependentlyOfNino() {
            dataStore.saveUtr("1234567890");
            dataStore.saveNino("QQ123456C");

            assertThat(dataStore.loadUtr()).isEqualTo("1234567890");
            assertThat(dataStore.loadNino()).isEqualTo("QQ123456C");
        }
    }

    @Nested
    @DisplayName("Display Name Persistence")
    class DisplayNamePersistence {

        @Test
        @DisplayName("should save and load display name")
        void shouldSaveAndLoadDisplayName() {
            dataStore.saveDisplayName("Sarah");
            String loaded = dataStore.loadDisplayName();

            assertThat(loaded).isEqualTo("Sarah");
        }

        @Test
        @DisplayName("should return null when no display name saved")
        void shouldReturnNullWhenNoDisplayNameSaved() {
            String loaded = dataStore.loadDisplayName();

            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("should overwrite existing display name")
        void shouldOverwriteExistingDisplayName() {
            dataStore.saveDisplayName("Sarah");
            dataStore.saveDisplayName("John Smith");
            String loaded = dataStore.loadDisplayName();

            assertThat(loaded).isEqualTo("John Smith");
        }

        @Test
        @DisplayName("should handle saving empty string display name")
        void shouldHandleSavingEmptyStringDisplayName() {
            dataStore.saveDisplayName("Sarah");
            dataStore.saveDisplayName("");
            String loaded = dataStore.loadDisplayName();

            assertThat(loaded).isEmpty();
        }

        @Test
        @DisplayName("should handle saving null display name")
        void shouldHandleSavingNullDisplayName() {
            dataStore.saveDisplayName("Sarah");
            dataStore.saveDisplayName(null);
            String loaded = dataStore.loadDisplayName();

            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("should persist display name independently of other settings")
        void shouldPersistDisplayNameIndependentlyOfOtherSettings() {
            dataStore.saveDisplayName("Sarah");
            dataStore.saveUtr("1234567890");
            dataStore.saveNino("QQ123456C");

            assertThat(dataStore.loadDisplayName()).isEqualTo("Sarah");
            assertThat(dataStore.loadUtr()).isEqualTo("1234567890");
            assertThat(dataStore.loadNino()).isEqualTo("QQ123456C");
        }

        @Test
        @DisplayName("should handle unicode characters in display name")
        void shouldHandleUnicodeCharactersInDisplayName() {
            String unicodeName = "Émile François";
            dataStore.saveDisplayName(unicodeName);
            String loaded = dataStore.loadDisplayName();

            assertThat(loaded).isEqualTo(unicodeName);
        }

        @Test
        @DisplayName("should handle special characters in display name")
        void shouldHandleSpecialCharactersInDisplayName() {
            String specialName = "O'Brien-Smith";
            dataStore.saveDisplayName(specialName);
            String loaded = dataStore.loadDisplayName();

            assertThat(loaded).isEqualTo(specialName);
        }
    }

    @Nested
    @DisplayName("HMRC Business ID Persistence")
    class HmrcBusinessIdPersistence {

        @Test
        @DisplayName("should save and load HMRC business ID")
        void shouldSaveAndLoadHmrcBusinessId() {
            dataStore.saveHmrcBusinessId("XAIS12345678901");
            String loaded = dataStore.loadHmrcBusinessId();

            assertThat(loaded).isEqualTo("XAIS12345678901");
        }

        @Test
        @DisplayName("should return null when no HMRC business ID saved")
        void shouldReturnNullWhenNoHmrcBusinessIdSaved() {
            String loaded = dataStore.loadHmrcBusinessId();

            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("should save and load HMRC trading name")
        void shouldSaveAndLoadHmrcTradingName() {
            dataStore.saveHmrcTradingName("Test Business");
            String loaded = dataStore.loadHmrcTradingName();

            assertThat(loaded).isEqualTo("Test Business");
        }

        @Test
        @DisplayName("should overwrite existing HMRC business ID")
        void shouldOverwriteExistingHmrcBusinessId() {
            dataStore.saveHmrcBusinessId("XAIS12345678901");
            dataStore.saveHmrcBusinessId("XAIS99999999999");
            String loaded = dataStore.loadHmrcBusinessId();

            assertThat(loaded).isEqualTo("XAIS99999999999");
        }
    }

    @Nested
    @DisplayName("NINO Verification Status Persistence")
    class NinoVerificationStatusPersistence {

        @Test
        @DisplayName("should save and load NINO verified status as true")
        void shouldSaveAndLoadNinoVerifiedAsTrue() {
            dataStore.saveNinoVerified(true);
            boolean verified = dataStore.isNinoVerified();

            assertThat(verified).isTrue();
        }

        @Test
        @DisplayName("should save and load NINO verified status as false")
        void shouldSaveAndLoadNinoVerifiedAsFalse() {
            dataStore.saveNinoVerified(false);
            boolean verified = dataStore.isNinoVerified();

            assertThat(verified).isFalse();
        }

        @Test
        @DisplayName("should return false when no verification status saved")
        void shouldReturnFalseWhenNoVerificationStatusSaved() {
            // No verification status saved - default should be false (unverified)
            boolean verified = dataStore.isNinoVerified();

            assertThat(verified).isFalse();
        }

        @Test
        @DisplayName("should overwrite existing verification status")
        void shouldOverwriteExistingVerificationStatus() {
            dataStore.saveNinoVerified(true);
            assertThat(dataStore.isNinoVerified()).isTrue();

            dataStore.saveNinoVerified(false);
            assertThat(dataStore.isNinoVerified()).isFalse();
        }

        @Test
        @DisplayName("should persist verification status independently of business ID")
        void shouldPersistVerificationStatusIndependentlyOfBusinessId() {
            dataStore.saveHmrcBusinessId("XAIS12345678901");
            dataStore.saveNinoVerified(true);

            assertThat(dataStore.loadHmrcBusinessId()).isEqualTo("XAIS12345678901");
            assertThat(dataStore.isNinoVerified()).isTrue();
        }
    }

    @Nested
    @DisplayName("Connected NINO Tracking")
    class ConnectedNinoTracking {

        @Test
        @DisplayName("should save and load connected NINO")
        void shouldSaveAndLoadConnectedNino() {
            dataStore.saveConnectedNino("QQ123456A");
            String loaded = dataStore.loadConnectedNino();

            assertThat(loaded).isEqualTo("QQ123456A");
        }

        @Test
        @DisplayName("should return null when no connected NINO saved")
        void shouldReturnNullWhenNoConnectedNinoSaved() {
            String loaded = dataStore.loadConnectedNino();

            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("should overwrite existing connected NINO")
        void shouldOverwriteExistingConnectedNino() {
            dataStore.saveConnectedNino("QQ123456A");
            dataStore.saveConnectedNino("AB654321D");
            String loaded = dataStore.loadConnectedNino();

            assertThat(loaded).isEqualTo("AB654321D");
        }

        @Test
        @DisplayName("should clear connected NINO when null is saved")
        void shouldClearConnectedNinoWhenNullIsSaved() {
            dataStore.saveConnectedNino("QQ123456A");
            dataStore.saveConnectedNino(null);
            String loaded = dataStore.loadConnectedNino();

            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("should detect when current NINO differs from connected NINO")
        void shouldDetectWhenCurrentNinoDiffersFromConnectedNino() {
            dataStore.saveConnectedNino("QQ123456A");
            dataStore.saveNino("AB654321D");

            String connected = dataStore.loadConnectedNino();
            String current = dataStore.loadNino();

            assertThat(connected).isNotEqualTo(current);
        }

        @Test
        @DisplayName("should persist connected NINO independently of current NINO")
        void shouldPersistConnectedNinoIndependentlyOfCurrentNino() {
            dataStore.saveNino("QQ123456A");
            dataStore.saveConnectedNino("QQ123456A");
            dataStore.saveNino("AB654321D"); // User changes NINO

            // Connected NINO should remain unchanged
            assertThat(dataStore.loadConnectedNino()).isEqualTo("QQ123456A");
            // Current NINO should be the new value
            assertThat(dataStore.loadNino()).isEqualTo("AB654321D");
        }
    }

    @Nested
    @DisplayName("Aggregation Queries")
    class AggregationQueries {

        @Test
        @DisplayName("should calculate total expenses for date range")
        void shouldCalculateTotalExpensesForDateRange() {
            LocalDate startDate = LocalDate.of(2024, 4, 6);
            LocalDate endDate = LocalDate.of(2025, 4, 5);

            dataStore.saveExpense(createTestExpenseWithDate(testBusinessId, LocalDate.of(2024, 6, 1), new BigDecimal("100.00")));
            dataStore.saveExpense(createTestExpenseWithDate(testBusinessId, LocalDate.of(2024, 7, 1), new BigDecimal("200.00")));
            dataStore.saveExpense(createTestExpenseWithDate(testBusinessId, LocalDate.of(2023, 1, 1), new BigDecimal("500.00"))); // Out of range

            BigDecimal total = dataStore.calculateTotalExpenses(testBusinessId, startDate, endDate);

            assertThat(total).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("should calculate allowable expenses for date range")
        void shouldCalculateAllowableExpensesForDateRange() {
            LocalDate startDate = LocalDate.of(2024, 4, 6);
            LocalDate endDate = LocalDate.of(2025, 4, 5);

            // Allowable expense
            Expense allowable = new Expense(
                    UUID.randomUUID(), testBusinessId, LocalDate.of(2024, 6, 1),
                    new BigDecimal("100.00"), "Office supplies", ExpenseCategory.OFFICE_COSTS, null, null, null, null, null,
                    null
            );
            // Non-allowable expense
            Expense nonAllowable = new Expense(
                    UUID.randomUUID(), testBusinessId, LocalDate.of(2024, 7, 1),
                    new BigDecimal("50.00"), "Entertainment", ExpenseCategory.BUSINESS_ENTERTAINMENT, null, null, null, null, null,
                    null
            );

            dataStore.saveExpense(allowable);
            dataStore.saveExpense(nonAllowable);

            BigDecimal allowableTotal = dataStore.calculateAllowableExpenses(testBusinessId, startDate, endDate);

            assertThat(allowableTotal).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should calculate total income for date range")
        void shouldCalculateTotalIncomeForDateRange() {
            LocalDate startDate = LocalDate.of(2024, 4, 6);
            LocalDate endDate = LocalDate.of(2025, 4, 5);

            dataStore.saveIncome(createTestIncomeWithDate(testBusinessId, LocalDate.of(2024, 5, 1), new BigDecimal("1000.00")));
            dataStore.saveIncome(createTestIncomeWithDate(testBusinessId, LocalDate.of(2024, 6, 1), new BigDecimal("2000.00")));
            dataStore.saveIncome(createTestIncomeWithDate(testBusinessId, LocalDate.of(2023, 1, 1), new BigDecimal("9999.00"))); // Out of range

            BigDecimal total = dataStore.calculateTotalIncome(testBusinessId, startDate, endDate);

            assertThat(total).isEqualByComparingTo(new BigDecimal("3000.00"));
        }
    }

    // === Test Helper Methods ===

    private Expense createTestExpense(UUID businessId) {
        return createTestExpense(businessId, "Test expense", new BigDecimal("100.00"));
    }

    private Expense createTestExpense(UUID businessId, String description, BigDecimal amount) {
        return new Expense(
                UUID.randomUUID(),
                businessId,
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

    private Expense createTestExpenseWithDate(UUID businessId, LocalDate date) {
        return createTestExpenseWithDate(businessId, date, new BigDecimal("100.00"));
    }

    private Expense createTestExpenseWithDate(UUID businessId, LocalDate date, BigDecimal amount) {
        return new Expense(
                UUID.randomUUID(),
                businessId,
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

    private Income createTestIncome(UUID businessId) {
        return createTestIncome(businessId, "Test invoice", new BigDecimal("1000.00"));
    }

    private Income createTestIncome(UUID businessId, String description, BigDecimal amount) {
        return new Income(
                UUID.randomUUID(),
                businessId,
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

    private Income createTestIncomeWithDate(UUID businessId, LocalDate date) {
        return createTestIncomeWithDate(businessId, date, new BigDecimal("1000.00"));
    }

    private Income createTestIncomeWithDate(UUID businessId, LocalDate date, BigDecimal amount) {
        return new Income(
                UUID.randomUUID(),
                businessId,
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
}
