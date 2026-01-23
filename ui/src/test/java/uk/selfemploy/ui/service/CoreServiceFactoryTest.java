package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.core.service.ReceiptStorageService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CoreServiceFactory.
 * Tests that the factory provides all required services for standalone mode.
 */
@DisplayName("CoreServiceFactory")
class CoreServiceFactoryTest {

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
    @DisplayName("Service Provisioning")
    class ServiceProvisioning {

        @Test
        @DisplayName("should provide ExpenseService")
        void shouldProvideExpenseService() {
            ExpenseService service = CoreServiceFactory.getExpenseService();

            assertThat(service).isNotNull();
            assertThat(service).isInstanceOf(SqliteExpenseService.class);
        }

        @Test
        @DisplayName("should provide IncomeService")
        void shouldProvideIncomeService() {
            IncomeService service = CoreServiceFactory.getIncomeService();

            assertThat(service).isNotNull();
            assertThat(service).isInstanceOf(SqliteIncomeService.class);
        }

        @Test
        @DisplayName("should provide ReceiptStorageService")
        void shouldProvideReceiptStorageService() {
            ReceiptStorageService service = CoreServiceFactory.getReceiptStorageService();

            assertThat(service).isNotNull();
            assertThat(service.getStorageDirectory()).isNotNull();
        }

        @Test
        @DisplayName("should provide default business ID")
        void shouldProvideDefaultBusinessId() {
            UUID businessId = CoreServiceFactory.getDefaultBusinessId();

            assertThat(businessId).isNotNull();
        }
    }

    @Nested
    @DisplayName("Singleton Behavior")
    class SingletonBehavior {

        @Test
        @DisplayName("should return same ExpenseService instance")
        void shouldReturnSameExpenseServiceInstance() {
            ExpenseService first = CoreServiceFactory.getExpenseService();
            ExpenseService second = CoreServiceFactory.getExpenseService();

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("should return same IncomeService instance")
        void shouldReturnSameIncomeServiceInstance() {
            IncomeService first = CoreServiceFactory.getIncomeService();
            IncomeService second = CoreServiceFactory.getIncomeService();

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("should return same ReceiptStorageService instance")
        void shouldReturnSameReceiptStorageServiceInstance() {
            ReceiptStorageService first = CoreServiceFactory.getReceiptStorageService();
            ReceiptStorageService second = CoreServiceFactory.getReceiptStorageService();

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("should return same business ID")
        void shouldReturnSameBusinessId() {
            UUID first = CoreServiceFactory.getDefaultBusinessId();
            UUID second = CoreServiceFactory.getDefaultBusinessId();

            assertThat(first).isEqualTo(second);
        }
    }

    @Nested
    @DisplayName("Initialization State")
    class InitializationState {

        @Test
        @DisplayName("should not be initialized after shutdown")
        void shouldNotBeInitializedAfterShutdown() {
            CoreServiceFactory.getExpenseService();
            assertThat(CoreServiceFactory.isInitialized()).isTrue();

            CoreServiceFactory.shutdown();

            assertThat(CoreServiceFactory.isInitialized()).isFalse();
        }

        @Test
        @DisplayName("should be initialized after getting any service")
        void shouldBeInitializedAfterGettingAnyService() {
            assertThat(CoreServiceFactory.isInitialized()).isFalse();

            CoreServiceFactory.getExpenseService();

            assertThat(CoreServiceFactory.isInitialized()).isTrue();
        }
    }

    @Nested
    @DisplayName("ReceiptStorageService Integration")
    class ReceiptStorageServiceIntegration {

        @Test
        @DisplayName("should have valid storage directory")
        void shouldHaveValidStorageDirectory() {
            ReceiptStorageService service = CoreServiceFactory.getReceiptStorageService();

            assertThat(service.getStorageDirectory()).isNotNull();
            // Path should contain 'receipts' as the last component
            assertThat(service.getStorageDirectory().getFileName().toString())
                    .isEqualTo("receipts");
        }

        @Test
        @DisplayName("should create storage directory in user-specific location")
        void shouldCreateStorageDirectoryInUserSpecificLocation() {
            ReceiptStorageService service = CoreServiceFactory.getReceiptStorageService();
            String path = service.getStorageDirectory().toString();

            // Should be in user's home or app data directory
            String userHome = System.getProperty("user.home");
            assertThat(path).containsIgnoringCase(userHome.substring(userHome.lastIndexOf('/') + 1));
        }
    }
}
