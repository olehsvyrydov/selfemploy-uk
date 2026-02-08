package uk.selfemploy.ui.service;

import uk.selfemploy.core.export.DataExportService;
import uk.selfemploy.core.export.DataImportService;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.core.service.PrivacyAcknowledgmentService;
import uk.selfemploy.core.service.ReceiptStorageService;
import uk.selfemploy.core.service.TermsAcceptanceService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Factory for creating core service instances in the UI layer.
 * Since the UI doesn't use Quarkus CDI, we manually construct the services.
 *
 * All services use SQLite for persistence to ensure data is never lost.
 * The business ID is persisted so it remains consistent across app restarts.
 */
public final class CoreServiceFactory {

    private static final Logger LOG = Logger.getLogger(CoreServiceFactory.class.getName());

    private static SqliteExpenseService expenseService;
    private static SqliteIncomeService incomeService;
    private static ReceiptStorageService receiptStorageService;
    private static TermsAcceptanceService termsAcceptanceService;
    private static PrivacyAcknowledgmentService privacyAcknowledgmentService;
    private static DataExportService dataExportService;
    private static DataImportService dataImportService;
    private static UiDuplicateDetectionService duplicateDetectionService;
    private static UiQuarterlySubmissionService quarterlySubmissionService;
    private static SqliteBankTransactionService bankTransactionService;
    private static ImportOrchestrationService importOrchestrationService;
    private static UUID defaultBusinessId;

    private CoreServiceFactory() {
        // Utility class
    }

    /**
     * Gets or creates the singleton ExpenseService instance.
     * Uses SQLite for persistence - data survives app restarts.
     *
     * @return The ExpenseService instance (SQLite-backed implementation)
     */
    public static synchronized ExpenseService getExpenseService() {
        if (expenseService == null) {
            UUID businessId = getDefaultBusinessId();
            LOG.info("Creating SQLite ExpenseService for business: " + businessId);
            expenseService = new SqliteExpenseService(businessId);
        }
        return expenseService;
    }

    /**
     * Gets or creates the singleton IncomeService instance.
     * Uses SQLite for persistence - data survives app restarts.
     *
     * @return The IncomeService instance (SQLite-backed implementation)
     */
    public static synchronized IncomeService getIncomeService() {
        if (incomeService == null) {
            UUID businessId = getDefaultBusinessId();
            LOG.info("Creating SQLite IncomeService for business: " + businessId);
            incomeService = new SqliteIncomeService(businessId);
        }
        return incomeService;
    }

    /**
     * Gets or creates the singleton ReceiptStorageService instance.
     *
     * @return The ReceiptStorageService instance
     */
    public static synchronized ReceiptStorageService getReceiptStorageService() {
        if (receiptStorageService == null) {
            LOG.info("Creating ReceiptStorageService");
            Path storagePath = resolveReceiptStoragePath();
            receiptStorageService = new ReceiptStorageService(storagePath);
        }
        return receiptStorageService;
    }

    /**
     * Gets or creates the singleton TermsAcceptanceService instance.
     * Uses SQLite for persistence - data survives app restarts.
     *
     * @return The TermsAcceptanceService instance (SQLite-backed implementation)
     */
    public static synchronized TermsAcceptanceService getTermsAcceptanceService() {
        if (termsAcceptanceService == null) {
            LOG.info("Creating SQLite TermsAcceptanceService");
            termsAcceptanceService = new SqliteTermsAcceptanceService();
        }
        return termsAcceptanceService;
    }

    /**
     * Gets or creates the singleton PrivacyAcknowledgmentService instance.
     * Uses SQLite for persistence - data survives app restarts.
     *
     * @return The PrivacyAcknowledgmentService instance (SQLite-backed implementation)
     */
    public static synchronized PrivacyAcknowledgmentService getPrivacyAcknowledgmentService() {
        if (privacyAcknowledgmentService == null) {
            LOG.info("Creating SQLite PrivacyAcknowledgmentService");
            privacyAcknowledgmentService = new SqlitePrivacyAcknowledgmentService();
        }
        return privacyAcknowledgmentService;
    }

    /**
     * Gets or creates the singleton DataExportService instance.
     *
     * @return The DataExportService instance
     */
    public static synchronized DataExportService getDataExportService() {
        if (dataExportService == null) {
            LOG.info("Creating DataExportService");
            dataExportService = new DataExportService(
                getIncomeService(),
                getExpenseService()
            );
        }
        return dataExportService;
    }

    /**
     * Gets or creates the singleton DataImportService instance.
     *
     * @return The DataImportService instance
     */
    public static synchronized DataImportService getDataImportService() {
        if (dataImportService == null) {
            LOG.info("Creating DataImportService");
            dataImportService = new DataImportService(
                getIncomeService(),
                getExpenseService()
            );
        }
        return dataImportService;
    }

    /**
     * Gets or creates the singleton UiDuplicateDetectionService instance.
     * Used for Import Review UI to detect duplicates before importing.
     *
     * @return The UiDuplicateDetectionService instance
     */
    public static synchronized UiDuplicateDetectionService getDuplicateDetectionService() {
        if (duplicateDetectionService == null) {
            LOG.info("Creating UiDuplicateDetectionService");
            duplicateDetectionService = new UiDuplicateDetectionService(
                getIncomeService(),
                getExpenseService(),
                getDefaultBusinessId()
            );
        }
        return duplicateDetectionService;
    }

    /**
     * Gets or creates the singleton SqliteBankTransactionService instance.
     * Used for the Transaction Review Dashboard.
     *
     * @return The SqliteBankTransactionService instance
     */
    public static synchronized SqliteBankTransactionService getBankTransactionService() {
        if (bankTransactionService == null) {
            UUID businessId = getDefaultBusinessId();
            LOG.info("Creating SQLite BankTransactionService for business: " + businessId);
            bankTransactionService = new SqliteBankTransactionService(businessId);
        }
        return bankTransactionService;
    }

    /**
     * Gets or creates the singleton ImportOrchestrationService instance.
     * Orchestrates CSV file loading, parsing, and transaction import.
     *
     * @return The ImportOrchestrationService instance
     */
    public static synchronized ImportOrchestrationService getImportOrchestrationService() {
        if (importOrchestrationService == null) {
            LOG.info("Creating ImportOrchestrationService");
            importOrchestrationService = new ImportOrchestrationService(
                new CsvTransactionParser(),
                getIncomeService(),
                getExpenseService(),
                getDefaultBusinessId()
            );
        }
        return importOrchestrationService;
    }

    /**
     * Gets or creates the singleton UiQuarterlySubmissionService instance.
     * Configured with NINO from SQLite settings and the default business ID.
     *
     * @return The UiQuarterlySubmissionService instance
     */
    public static synchronized UiQuarterlySubmissionService getQuarterlySubmissionService() {
        if (quarterlySubmissionService == null) {
            LOG.info("Creating UiQuarterlySubmissionService");
            quarterlySubmissionService = new UiQuarterlySubmissionService();
            // Load HMRC business ID from SQLite if previously synced
            String hmrcBusinessId = SqliteDataStore.getInstance().loadHmrcBusinessId();
            if (hmrcBusinessId != null && !hmrcBusinessId.isBlank()) {
                quarterlySubmissionService.setHmrcBusinessId(hmrcBusinessId);
                LOG.info("Loaded HMRC business ID from settings");
            }

            // Load NINO from SQLite if previously saved
            String nino = SqliteDataStore.getInstance().loadNino();
            if (nino != null && !nino.isBlank()) {
                quarterlySubmissionService.setNino(nino);
                LOG.info("Loaded NINO from settings");
            }
        }
        return quarterlySubmissionService;
    }

    /**
     * Gets the default business ID for standalone mode.
     * The business ID is persisted to SQLite so it remains constant across app restarts.
     *
     * @return The default business ID
     */
    public static synchronized UUID getDefaultBusinessId() {
        if (defaultBusinessId == null) {
            // Try to load from SQLite first
            SqliteDataStore dataStore = SqliteDataStore.getInstance();
            defaultBusinessId = dataStore.loadBusinessId();

            if (defaultBusinessId == null) {
                // First run - generate and persist new ID
                defaultBusinessId = UUID.randomUUID();
                dataStore.saveBusinessId(defaultBusinessId);
                dataStore.ensureBusinessExists(defaultBusinessId);
                LOG.info("Created and persisted new business ID: " + defaultBusinessId);
            } else {
                LOG.info("Loaded existing business ID: " + defaultBusinessId);
            }
        }
        return defaultBusinessId;
    }

    /**
     * Resolves the receipt storage path based on the operating system.
     */
    private static Path resolveReceiptStoragePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        Path basePath;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            basePath = appData != null
                ? Paths.get(appData, "SelfEmployment")
                : Paths.get(userHome, "AppData", "Roaming", "SelfEmployment");
        } else if (os.contains("mac")) {
            basePath = Paths.get(userHome, "Library", "Application Support", "SelfEmployment");
        } else {
            String xdgData = System.getenv("XDG_DATA_HOME");
            basePath = xdgData != null
                ? Paths.get(xdgData, "selfemployment")
                : Paths.get(userHome, ".local", "share", "selfemployment");
        }

        return basePath.resolve("receipts");
    }

    /**
     * Shuts down all services, releasing memory references.
     * Data remains safely persisted in SQLite for next app startup.
     * Should be called when the application is closing.
     */
    public static synchronized void shutdown() {
        expenseService = null;
        incomeService = null;
        receiptStorageService = null;
        termsAcceptanceService = null;
        privacyAcknowledgmentService = null;
        dataExportService = null;
        dataImportService = null;
        duplicateDetectionService = null;
        quarterlySubmissionService = null;
        bankTransactionService = null;
        importOrchestrationService = null;
        defaultBusinessId = null;
        LOG.info("CoreServiceFactory shutdown - data persisted");
    }

    /**
     * Returns true if services are currently initialized.
     */
    public static boolean isInitialized() {
        return expenseService != null || incomeService != null || receiptStorageService != null
                || termsAcceptanceService != null || privacyAcknowledgmentService != null
                || dataExportService != null || dataImportService != null
                || duplicateDetectionService != null || quarterlySubmissionService != null
                || bankTransactionService != null || importOrchestrationService != null;
    }

    /**
     * Sets the default business ID for testing purposes only.
     * This method bypasses SQLite persistence and should only be used in tests.
     *
     * @param businessId The business ID to set
     */
    public static synchronized void setDefaultBusinessIdForTesting(UUID businessId) {
        defaultBusinessId = businessId;
    }
}
