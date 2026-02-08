package uk.selfemploy.ui.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test support utility for SQLite database operations.
 * This class provides test-only functionality like clearing data and resetting state.
 *
 * <p>This class should ONLY be used in test code, never in production.
 * It uses reflection to access internal state for testing purposes.
 */
public final class SqliteTestSupport {

    private static final Logger LOG = Logger.getLogger(SqliteTestSupport.class.getName());
    private static boolean testModeEnabled = false;

    private SqliteTestSupport() {
        // Utility class
    }

    /**
     * Enables test mode with in-memory database.
     * Must be called BEFORE any SqliteDataStore.getInstance() call.
     *
     * <p>This is for test setup only. Production code must never call this.
     */
    public static synchronized void enableTestMode() {
        try {
            // Reset the singleton first
            resetInstance();

            // Set test mode via reflection
            Field testModeField = SqliteDataStore.class.getDeclaredField("testMode");
            testModeField.setAccessible(true);
            testModeField.set(null, true);
            testModeEnabled = true;

            LOG.info("Test mode enabled - using in-memory database");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to enable test mode", e);
            throw new IllegalStateException("Failed to enable test mode", e);
        }
    }

    /**
     * Disables test mode and resets to file-based database.
     * Call this in @AfterAll to clean up.
     */
    public static synchronized void disableTestMode() {
        try {
            // Reset the singleton
            resetInstance();

            // Disable test mode via reflection
            Field testModeField = SqliteDataStore.class.getDeclaredField("testMode");
            testModeField.setAccessible(true);
            testModeField.set(null, false);
            testModeEnabled = false;

            LOG.info("Test mode disabled - using file-based database");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to disable test mode", e);
            throw new IllegalStateException("Failed to disable test mode", e);
        }
    }

    /**
     * Resets the SqliteDataStore singleton instance.
     * Closes the current connection and clears the instance.
     */
    public static synchronized void resetInstance() {
        try {
            Field instanceField = SqliteDataStore.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            SqliteDataStore currentInstance = (SqliteDataStore) instanceField.get(null);

            if (currentInstance != null) {
                currentInstance.close();
                instanceField.set(null, null);
            }

            LOG.fine("SqliteDataStore instance reset");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to reset SqliteDataStore instance", e);
        }
    }

    /**
     * Clears all data from the database.
     * This deletes all expenses, income, and settings.
     *
     * <p>WARNING: This is destructive and should ONLY be used in tests.
     */
    public static synchronized void clearAllData() {
        try {
            SqliteDataStore dataStore = SqliteDataStore.getInstance();
            Connection connection = getConnection(dataStore);

            if (connection != null) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("DELETE FROM bank_transactions");
                    stmt.execute("DELETE FROM expenses");
                    stmt.execute("DELETE FROM income");
                    stmt.execute("DELETE FROM settings");
                    stmt.execute("DELETE FROM business");
                    LOG.info("Cleared all data from test database");
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to clear data", e);
            throw new IllegalStateException("Failed to clear test data", e);
        }
    }

    /**
     * Returns true if test mode is enabled.
     */
    public static boolean isTestModeEnabled() {
        return testModeEnabled;
    }

    /**
     * Convenience method to set up a clean test environment.
     * Enables test mode and clears any existing data.
     *
     * <p>Call this in @BeforeAll:
     * <pre>
     * @BeforeAll
     * static void setUpClass() {
     *     SqliteTestSupport.setUpTestEnvironment();
     * }
     * </pre>
     */
    public static synchronized void setUpTestEnvironment() {
        enableTestMode();
    }

    /**
     * Convenience method to tear down the test environment.
     * Disables test mode and resets state.
     *
     * <p>Call this in @AfterAll:
     * <pre>
     * @AfterAll
     * static void tearDownClass() {
     *     SqliteTestSupport.tearDownTestEnvironment();
     * }
     * </pre>
     */
    public static synchronized void tearDownTestEnvironment() {
        disableTestMode();
    }

    /**
     * Resets the test environment between tests.
     * Clears data but keeps test mode enabled.
     *
     * <p>Call this in @BeforeEach or @AfterEach:
     * <pre>
     * @BeforeEach
     * void setUp() {
     *     SqliteTestSupport.resetTestData();
     * }
     * </pre>
     */
    public static synchronized void resetTestData() {
        if (!testModeEnabled) {
            throw new IllegalStateException(
                    "Test mode not enabled. Call setUpTestEnvironment() in @BeforeAll first.");
        }
        clearAllData();
    }

    /**
     * Resets CoreServiceFactory state for testing.
     * Clears all cached services so they will be recreated.
     */
    public static synchronized void resetCoreServiceFactory() {
        try {
            // Clear all service references
            setStaticField(CoreServiceFactory.class, "expenseService", null);
            setStaticField(CoreServiceFactory.class, "incomeService", null);
            setStaticField(CoreServiceFactory.class, "receiptStorageService", null);
            setStaticField(CoreServiceFactory.class, "defaultBusinessId", null);

            LOG.fine("CoreServiceFactory reset");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to reset CoreServiceFactory", e);
        }
    }

    private static Connection getConnection(SqliteDataStore dataStore) {
        try {
            Field connectionField = SqliteDataStore.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            return (Connection) connectionField.get(dataStore);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to get connection via reflection", e);
            return null;
        }
    }

    private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
