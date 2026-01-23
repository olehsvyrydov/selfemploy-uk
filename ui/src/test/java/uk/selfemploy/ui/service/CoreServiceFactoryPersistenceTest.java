package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CoreServiceFactory persistence behavior.
 * Verifies that business ID and data persist across factory restarts.
 */
@DisplayName("CoreServiceFactory Persistence")
class CoreServiceFactoryPersistenceTest {

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
        SqliteTestSupport.resetCoreServiceFactory();
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.resetCoreServiceFactory();
        SqliteTestSupport.resetTestData();
    }

    @Nested
    @DisplayName("Business ID Persistence")
    class BusinessIdPersistence {

        @Test
        @DisplayName("should persist business ID across factory restarts")
        void shouldPersistBusinessIdAcrossFactoryRestarts() {
            // Get initial business ID
            UUID firstId = CoreServiceFactory.getDefaultBusinessId();
            assertThat(firstId).isNotNull();

            // Shutdown and reinitialize
            CoreServiceFactory.shutdown();

            // Should get the same business ID
            UUID secondId = CoreServiceFactory.getDefaultBusinessId();
            assertThat(secondId).isEqualTo(firstId);
        }

        @Test
        @DisplayName("should generate new ID only on first run")
        void shouldGenerateNewIdOnlyOnFirstRun() {
            // First call should generate and persist
            UUID businessId = CoreServiceFactory.getDefaultBusinessId();
            assertThat(businessId).isNotNull();

            // Verify it was saved to database
            UUID fromDb = SqliteDataStore.getInstance().loadBusinessId();
            assertThat(fromDb).isEqualTo(businessId);
        }
    }

    @Nested
    @DisplayName("Expense Data Persistence")
    class ExpenseDataPersistence {

        @Test
        @DisplayName("should persist expenses across factory restarts")
        void shouldPersistExpensesAcrossFactoryRestarts() {
            // Create expense
            UUID businessId = CoreServiceFactory.getDefaultBusinessId();
            ExpenseService service = CoreServiceFactory.getExpenseService();
            service.create(businessId, LocalDate.now().minusDays(1), new BigDecimal("150.00"),
                    "Test expense", ExpenseCategory.OFFICE_COSTS, null, null);

            // Shutdown factory
            CoreServiceFactory.shutdown();

            // Get service again - should have the expense
            ExpenseService newService = CoreServiceFactory.getExpenseService();
            // Note: The business ID should be the same
            UUID sameBid = CoreServiceFactory.getDefaultBusinessId();
            assertThat(sameBid).isEqualTo(businessId);

            // Count should reflect persisted data
            assertThat(((SqliteExpenseService) newService).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not lose expenses on service restart")
        void shouldNotLoseExpensesOnServiceRestart() {
            UUID businessId = CoreServiceFactory.getDefaultBusinessId();
            ExpenseService service = CoreServiceFactory.getExpenseService();

            // Create multiple expenses
            service.create(businessId, LocalDate.now().minusDays(1), new BigDecimal("100.00"),
                    "Expense 1", ExpenseCategory.OFFICE_COSTS, null, null);
            service.create(businessId, LocalDate.now().minusDays(2), new BigDecimal("200.00"),
                    "Expense 2", ExpenseCategory.TRAVEL, null, null);
            service.create(businessId, LocalDate.now().minusDays(3), new BigDecimal("300.00"),
                    "Expense 3", ExpenseCategory.PROFESSIONAL_FEES, null, null);

            // Simulate app restart
            CoreServiceFactory.shutdown();

            // Verify all expenses are preserved
            ExpenseService newService = CoreServiceFactory.getExpenseService();
            assertThat(((SqliteExpenseService) newService).count()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Income Data Persistence")
    class IncomeDataPersistence {

        @Test
        @DisplayName("should persist income across factory restarts")
        void shouldPersistIncomeAcrossFactoryRestarts() {
            // Create income
            UUID businessId = CoreServiceFactory.getDefaultBusinessId();
            IncomeService service = CoreServiceFactory.getIncomeService();
            service.create(businessId, LocalDate.now().minusDays(1), new BigDecimal("1500.00"),
                    "Test invoice", IncomeCategory.SALES, "INV-001");

            // Shutdown factory
            CoreServiceFactory.shutdown();

            // Get service again - should have the income
            IncomeService newService = CoreServiceFactory.getIncomeService();

            // Count should reflect persisted data
            assertThat(((SqliteIncomeService) newService).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not lose income on service restart")
        void shouldNotLoseIncomeOnServiceRestart() {
            UUID businessId = CoreServiceFactory.getDefaultBusinessId();
            IncomeService service = CoreServiceFactory.getIncomeService();

            // Create multiple income entries
            service.create(businessId, LocalDate.now().minusDays(1), new BigDecimal("1000.00"),
                    "Invoice 1", IncomeCategory.SALES, "INV-001");
            service.create(businessId, LocalDate.now().minusDays(2), new BigDecimal("2000.00"),
                    "Invoice 2", IncomeCategory.SALES, "INV-002");

            // Simulate app restart
            CoreServiceFactory.shutdown();

            // Verify all income is preserved
            IncomeService newService = CoreServiceFactory.getIncomeService();
            assertThat(((SqliteIncomeService) newService).count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Service Type Verification")
    class ServiceTypeVerification {

        @Test
        @DisplayName("should provide SQLite expense service")
        void shouldProvideSqliteExpenseService() {
            ExpenseService service = CoreServiceFactory.getExpenseService();
            assertThat(service).isInstanceOf(SqliteExpenseService.class);
        }

        @Test
        @DisplayName("should provide SQLite income service")
        void shouldProvideSqliteIncomeService() {
            IncomeService service = CoreServiceFactory.getIncomeService();
            assertThat(service).isInstanceOf(SqliteIncomeService.class);
        }
    }
}
