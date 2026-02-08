package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ReviewStatus;
import uk.selfemploy.core.reconciliation.MatchTier;
import uk.selfemploy.core.reconciliation.ReconciliationMatch;
import uk.selfemploy.core.reconciliation.ReconciliationStatus;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite-based data store for persisting application data.
 * Stores data in the user's application data directory.
 *
 * <p>Storage locations:
 * <ul>
 *   <li>Windows: %APPDATA%/SelfEmployment/selfemploy.db</li>
 *   <li>macOS: ~/Library/Application Support/SelfEmployment/selfemploy.db</li>
 *   <li>Linux: ~/.local/share/selfemployment/selfemploy.db</li>
 * </ul>
 */
public final class SqliteDataStore {

    private static final Logger LOG = Logger.getLogger(SqliteDataStore.class.getName());
    private static final String DB_FILE = "selfemploy.db";

    // Package-private for test access via reflection in SqliteTestSupport
    static SqliteDataStore instance;
    static boolean testMode = false;

    private final Path databasePath;
    private final boolean inMemory;
    private Connection connection;

    private SqliteDataStore() {
        this(false);
    }

    SqliteDataStore(boolean inMemory) {
        this.inMemory = inMemory;
        this.databasePath = inMemory ? null : resolveDatabasePath();
        if (!inMemory) {
            ensureDirectoryExists();
        }
        initializeDatabase();
    }

    /**
     * Gets the singleton instance.
     * Uses file-based SQLite for production.
     */
    public static synchronized SqliteDataStore getInstance() {
        if (instance == null) {
            instance = new SqliteDataStore(testMode);
        }
        return instance;
    }

    /**
     * Resolves the database path based on the operating system.
     */
    private Path resolveDatabasePath() {
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

        return basePath.resolve(DB_FILE);
    }

    /**
     * Ensures the parent directory exists.
     */
    private void ensureDirectoryExists() {
        try {
            Path parent = databasePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                LOG.info("Created data directory: " + parent);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create data directory", e);
        }
    }

    /**
     * Initializes the database connection and creates tables.
     */
    private void initializeDatabase() {
        try {
            String url = inMemory ? "jdbc:sqlite::memory:" : "jdbc:sqlite:" + databasePath.toAbsolutePath();
            connection = DriverManager.getConnection(url);
            LOG.info("Connected to SQLite database: " + (inMemory ? ":memory:" : databasePath));

            configureSqlite();
            createTables();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to initialize SQLite database", e);
        }
    }

    /**
     * Configures SQLite for optimal safety and performance.
     * - WAL mode: Better crash recovery and concurrent reads
     * - Foreign keys: Referential integrity
     * - Synchronous NORMAL: Good balance of safety/performance
     */
    private void configureSqlite() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Enable Write-Ahead Logging for better crash recovery
            stmt.execute("PRAGMA journal_mode = WAL");
            // Enable foreign key constraints
            stmt.execute("PRAGMA foreign_keys = ON");
            // Balanced sync mode (data-safe but not overly slow)
            stmt.execute("PRAGMA synchronous = NORMAL");
            // Wait up to 5 seconds if database is locked
            stmt.execute("PRAGMA busy_timeout = 5000");
            LOG.info("SQLite configured with WAL mode and foreign keys enabled");
        }
    }

    /**
     * Creates database tables if they don't exist.
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Settings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
            """);

            // Business table (to track business ID)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS business (
                    id TEXT PRIMARY KEY,
                    name TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                )
            """);

            // Expenses table with foreign key to business
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id TEXT PRIMARY KEY,
                    business_id TEXT NOT NULL,
                    date TEXT NOT NULL,
                    amount TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    receipt_path TEXT,
                    notes TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE
                )
            """);

            // Income table with foreign key to business
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS income (
                    id TEXT PRIMARY KEY,
                    business_id TEXT NOT NULL,
                    date TEXT NOT NULL,
                    amount TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    reference TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE
                )
            """);

            // Create indexes for better query performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expenses_business_date ON expenses(business_id, date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_income_business_date ON income(business_id, date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_income_category ON income(category)");

            // Terms acceptance table for SE-508
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS terms_acceptance (
                    id TEXT PRIMARY KEY,
                    tos_version TEXT NOT NULL,
                    accepted_at TEXT NOT NULL,
                    scroll_completed_at TEXT NOT NULL,
                    application_version TEXT NOT NULL
                )
            """);

            // Privacy acknowledgment table for SE-507
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS privacy_acknowledgment (
                    id TEXT PRIMARY KEY,
                    privacy_version TEXT NOT NULL,
                    acknowledged_at TEXT NOT NULL,
                    application_version TEXT NOT NULL
                )
            """);

            // Submissions table for BUG-10H-001: Submission History persistence
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS submissions (
                    id TEXT PRIMARY KEY,
                    business_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    tax_year_start INTEGER NOT NULL,
                    period_start TEXT NOT NULL,
                    period_end TEXT NOT NULL,
                    total_income TEXT NOT NULL,
                    total_expenses TEXT NOT NULL,
                    net_profit TEXT NOT NULL,
                    status TEXT NOT NULL,
                    hmrc_reference TEXT,
                    error_message TEXT,
                    submitted_at TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE,
                    CHECK (type IN ('QUARTERLY_Q1', 'QUARTERLY_Q2', 'QUARTERLY_Q3', 'QUARTERLY_Q4', 'ANNUAL')),
                    CHECK (status IN ('PENDING', 'SUBMITTED', 'ACCEPTED', 'REJECTED'))
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_submissions_business ON submissions(business_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_submissions_tax_year ON submissions(tax_year_start)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_submissions_status ON submissions(status)");

            // Bank transactions table for imported statement review
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bank_transactions (
                    id TEXT PRIMARY KEY,
                    business_id TEXT NOT NULL,
                    import_audit_id TEXT NOT NULL,
                    source_format_id TEXT,
                    date TEXT NOT NULL,
                    amount TEXT NOT NULL,
                    description TEXT NOT NULL,
                    account_last_four TEXT,
                    bank_transaction_id TEXT,
                    transaction_hash TEXT NOT NULL,
                    review_status TEXT NOT NULL DEFAULT 'PENDING',
                    income_id TEXT,
                    expense_id TEXT,
                    exclusion_reason TEXT,
                    is_business INTEGER,
                    confidence_score TEXT,
                    suggested_category TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
                    deleted_at TEXT,
                    deleted_by TEXT,
                    deletion_reason TEXT,
                    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE,
                    CHECK (review_status IN ('PENDING', 'CATEGORIZED', 'EXCLUDED', 'SKIPPED'))
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bank_tx_business_status ON bank_transactions(business_id, review_status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bank_tx_business_date ON bank_transactions(business_id, date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bank_tx_hash ON bank_transactions(transaction_hash)");

            // Immutable audit trail for all bank transaction state changes.
            // Required for MTD digital link compliance: every modification to
            // a bank transaction must be traceable.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transaction_modification_log (
                    id TEXT PRIMARY KEY,
                    bank_transaction_id TEXT NOT NULL,
                    modification_type TEXT NOT NULL,
                    field_name TEXT,
                    previous_value TEXT,
                    new_value TEXT,
                    modified_by TEXT NOT NULL,
                    modified_at TEXT NOT NULL,
                    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id),
                    CHECK (modification_type IN (
                        'CATEGORIZED', 'EXCLUDED', 'RECATEGORIZED',
                        'RESTORED', 'BUSINESS_PERSONAL_CHANGED', 'CATEGORY_CHANGED'
                    ))
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mod_log_bank_tx ON transaction_modification_log(bank_transaction_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mod_log_time ON transaction_modification_log(modified_at)");

            // Reconciliation matches table for detecting duplicates between
            // bank-imported and manually entered transactions.
            // Records are statutory and must never be hard-deleted.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reconciliation_matches (
                    id TEXT PRIMARY KEY,
                    bank_transaction_id TEXT NOT NULL,
                    manual_transaction_id TEXT NOT NULL,
                    manual_transaction_type TEXT NOT NULL CHECK(manual_transaction_type IN ('INCOME', 'EXPENSE')),
                    confidence REAL NOT NULL,
                    match_tier TEXT NOT NULL CHECK(match_tier IN ('LINKED', 'EXACT', 'LIKELY', 'POSSIBLE')),
                    status TEXT NOT NULL DEFAULT 'UNRESOLVED' CHECK(status IN ('UNRESOLVED', 'CONFIRMED', 'DISMISSED')),
                    business_id TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    resolved_at TEXT,
                    resolved_by TEXT,
                    UNIQUE(bank_transaction_id, manual_transaction_id, manual_transaction_type)
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_recon_business ON reconciliation_matches(business_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_recon_status ON reconciliation_matches(business_id, status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_recon_bank_tx ON reconciliation_matches(bank_transaction_id)");

            LOG.info("Database tables initialized");
        }
    }

    // === Settings Operations ===

    /**
     * Saves the business ID.
     */
    public synchronized void saveBusinessId(UUID businessId) {
        saveSetting("business_id", businessId != null ? businessId.toString() : null);
    }

    /**
     * Loads the business ID.
     */
    public synchronized UUID loadBusinessId() {
        String value = loadSetting("business_id");
        if (value != null && !value.isBlank()) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                LOG.warning("Invalid business ID in settings: " + value);
            }
        }
        return null;
    }

    private void saveSetting(String key, String value) {
        if (value == null) {
            // Delete the setting if value is null (to clear it)
            String sql = "DELETE FROM settings WHERE key = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, key);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Failed to delete setting: " + key, e);
            }
        } else {
            String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, key);
                pstmt.setString(2, value);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Failed to save setting: " + key, e);
            }
        }
    }

    private String loadSetting(String key) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to load setting: " + key, e);
        }
        return null;
    }

    // === NINO Operations ===

    /**
     * Saves the National Insurance Number.
     * The NINO is normalised to uppercase before storage.
     *
     * @param nino the NINO (e.g., "QQ123456C"), or null to clear
     */
    public synchronized void saveNino(String nino) {
        saveSetting("nino", nino != null ? nino.toUpperCase() : null);
    }

    /**
     * Loads the stored National Insurance Number.
     *
     * @return the NINO, or null if not set
     */
    public synchronized String loadNino() {
        return loadSetting("nino");
    }

    // === Display Name Operations ===

    /**
     * Saves the user's display name for personalization.
     *
     * @param displayName the display name (e.g., "Sarah", "John Smith"), or null to clear
     */
    public synchronized void saveDisplayName(String displayName) {
        saveSetting("display_name", displayName);
    }

    /**
     * Loads the stored display name.
     *
     * @return the display name, or null if not set
     */
    public synchronized String loadDisplayName() {
        return loadSetting("display_name");
    }

    // === UTR Operations ===

    /**
     * Saves the Unique Taxpayer Reference.
     *
     * @param utr the UTR (10 digits), or null to clear
     */
    public synchronized void saveUtr(String utr) {
        saveSetting("utr", utr);
    }

    /**
     * Loads the stored Unique Taxpayer Reference.
     *
     * @return the UTR, or null if not set
     */
    public synchronized String loadUtr() {
        return loadSetting("utr");
    }

    // === OAuth Token Operations (Sprint 12) ===

    /**
     * Saves OAuth tokens to persistent storage.
     * Note: Tokens should be encrypted in production (TD-XXX).
     *
     * @param accessToken the OAuth access token
     * @param refreshToken the OAuth refresh token
     * @param expiresIn seconds until access token expires
     * @param tokenType the token type (usually "bearer")
     * @param scope the granted scopes
     * @param issuedAt when the tokens were issued
     */
    public synchronized void saveOAuthTokens(String accessToken, String refreshToken,
                                             long expiresIn, String tokenType,
                                             String scope, Instant issuedAt) {
        saveSetting("oauth_access_token", accessToken);
        saveSetting("oauth_refresh_token", refreshToken);
        saveSetting("oauth_expires_in", String.valueOf(expiresIn));
        saveSetting("oauth_token_type", tokenType);
        saveSetting("oauth_scope", scope);
        saveSetting("oauth_issued_at", issuedAt != null ? issuedAt.toString() : null);
        LOG.info("OAuth tokens saved to persistent storage");
    }

    /**
     * Loads OAuth tokens from persistent storage.
     *
     * @return array of [accessToken, refreshToken, expiresIn, tokenType, scope, issuedAt],
     *         or null if not stored
     */
    public synchronized String[] loadOAuthTokens() {
        String accessToken = loadSetting("oauth_access_token");
        if (accessToken == null) {
            return null;
        }
        return new String[] {
            accessToken,
            loadSetting("oauth_refresh_token"),
            loadSetting("oauth_expires_in"),
            loadSetting("oauth_token_type"),
            loadSetting("oauth_scope"),
            loadSetting("oauth_issued_at")
        };
    }

    /**
     * Clears stored OAuth tokens.
     */
    public synchronized void clearOAuthTokens() {
        saveSetting("oauth_access_token", null);
        saveSetting("oauth_refresh_token", null);
        saveSetting("oauth_expires_in", null);
        saveSetting("oauth_token_type", null);
        saveSetting("oauth_scope", null);
        saveSetting("oauth_issued_at", null);
        LOG.info("OAuth tokens cleared from persistent storage");
    }

    /**
     * Checks if OAuth tokens are stored.
     *
     * @return true if tokens are stored
     */
    public synchronized boolean hasOAuthTokens() {
        return loadSetting("oauth_access_token") != null;
    }

    // === HMRC Business ID Operations ===

    /**
     * Saves the HMRC-assigned business ID (e.g., "XAIS12345678901").
     * This is different from the local UUID business ID used for SQLite FK relationships.
     *
     * @param hmrcBusinessId the HMRC business ID, or null to clear
     */
    public synchronized void saveHmrcBusinessId(String hmrcBusinessId) {
        saveSetting("hmrc_business_id", hmrcBusinessId);
    }

    /**
     * Loads the stored HMRC business ID.
     *
     * @return the HMRC business ID, or null if not set
     */
    public synchronized String loadHmrcBusinessId() {
        return loadSetting("hmrc_business_id");
    }

    /**
     * Saves the HMRC trading name associated with the business.
     *
     * @param tradingName the trading name, or null to clear
     */
    public synchronized void saveHmrcTradingName(String tradingName) {
        saveSetting("hmrc_trading_name", tradingName);
    }

    /**
     * Loads the stored HMRC trading name.
     *
     * @return the trading name, or null if not set
     */
    public synchronized String loadHmrcTradingName() {
        return loadSetting("hmrc_trading_name");
    }

    /**
     * Saves the NINO verification status.
     * When true, the NINO has been verified by HMRC (200 response with business ID).
     * When false, the NINO was not verified (404, 401, or using sandbox fallback).
     *
     * @param verified true if NINO was verified by HMRC, false otherwise
     */
    public synchronized void saveNinoVerified(boolean verified) {
        saveSetting("nino_verified", verified ? "true" : "false");
    }

    /**
     * Loads the NINO verification status.
     *
     * @return true if NINO was verified by HMRC, false otherwise (default false)
     */
    public synchronized boolean isNinoVerified() {
        String value = loadSetting("nino_verified");
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Saves the NINO that was used when the HMRC connection was established.
     * This is used to detect if the user changes their NINO after connecting.
     * In sandbox mode, we cannot verify if a new NINO is valid, so we track
     * the original connected NINO to warn users about changes.
     *
     * @param nino the NINO used during connection, or null to clear
     */
    public synchronized void saveConnectedNino(String nino) {
        saveSetting("connected_nino", nino);
    }

    /**
     * Loads the NINO that was used when the HMRC connection was established.
     *
     * @return the connected NINO, or null if not set
     */
    public synchronized String loadConnectedNino() {
        return loadSetting("connected_nino");
    }

    // === Expense Operations ===

    /**
     * Saves an expense to the database.
     */
    public synchronized void saveExpense(Expense expense) {
        String sql = """
            INSERT OR REPLACE INTO expenses
            (id, business_id, date, amount, description, category, receipt_path, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, expense.id().toString());
            pstmt.setString(2, expense.businessId().toString());
            pstmt.setString(3, expense.date().toString());
            pstmt.setString(4, expense.amount().toPlainString());
            pstmt.setString(5, expense.description());
            pstmt.setString(6, expense.category().name());
            pstmt.setString(7, expense.receiptPath());
            pstmt.setString(8, expense.notes());
            pstmt.executeUpdate();
            LOG.fine("Saved expense: " + expense.id());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save expense: " + expense.id(), e);
        }
    }

    /**
     * Loads all expenses from the database.
     */
    public synchronized List<Expense> loadAllExpenses() {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM expenses ORDER BY date DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                expenses.add(mapExpense(rs));
            }
            LOG.info("Loaded " + expenses.size() + " expenses from database");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to load expenses", e);
        }
        return expenses;
    }

    /**
     * Finds an expense by ID.
     */
    public synchronized Optional<Expense> findExpenseById(UUID id) {
        String sql = "SELECT * FROM expenses WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapExpense(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find expense: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Deletes an expense by ID.
     */
    public synchronized boolean deleteExpense(UUID id) {
        String sql = "DELETE FROM expenses WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to delete expense: " + id, e);
            return false;
        }
    }

    private Expense mapExpense(ResultSet rs) throws SQLException {
        return new Expense(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("business_id")),
            LocalDate.parse(rs.getString("date")),
            new BigDecimal(rs.getString("amount")),
            rs.getString("description"),
            ExpenseCategory.valueOf(rs.getString("category")),
            rs.getString("receipt_path"),
            rs.getString("notes"),
            null, // bankTransactionRef - not stored in SQLite yet
            null, // supplierRef - not stored in SQLite yet
            null, // invoiceNumber - not stored in SQLite yet
            null  // bankTransactionId - not stored in SQLite yet
        );
    }

    // === Income Operations ===

    /**
     * Saves an income entry to the database.
     */
    public synchronized void saveIncome(Income income) {
        String sql = """
            INSERT OR REPLACE INTO income
            (id, business_id, date, amount, description, category, reference)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, income.id().toString());
            pstmt.setString(2, income.businessId().toString());
            pstmt.setString(3, income.date().toString());
            pstmt.setString(4, income.amount().toPlainString());
            pstmt.setString(5, income.description());
            pstmt.setString(6, income.category().name());
            pstmt.setString(7, income.reference());
            pstmt.executeUpdate();
            LOG.fine("Saved income: " + income.id());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save income: " + income.id(), e);
        }
    }

    /**
     * Loads all income entries from the database.
     */
    public synchronized List<Income> loadAllIncome() {
        List<Income> incomeList = new ArrayList<>();
        String sql = "SELECT * FROM income ORDER BY date DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                incomeList.add(mapIncome(rs));
            }
            LOG.info("Loaded " + incomeList.size() + " income entries from database");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to load income", e);
        }
        return incomeList;
    }

    /**
     * Finds an income entry by ID.
     */
    public synchronized Optional<Income> findIncomeById(UUID id) {
        String sql = "SELECT * FROM income WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapIncome(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find income: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Deletes an income entry by ID.
     */
    public synchronized boolean deleteIncome(UUID id) {
        String sql = "DELETE FROM income WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to delete income: " + id, e);
            return false;
        }
    }

    private Income mapIncome(ResultSet rs) throws SQLException {
        return new Income(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("business_id")),
            LocalDate.parse(rs.getString("date")),
            new BigDecimal(rs.getString("amount")),
            rs.getString("description"),
            IncomeCategory.valueOf(rs.getString("category")),
            rs.getString("reference"),
            null, // bankTransactionRef - not stored in SQLite yet
            null, // invoiceNumber - not stored in SQLite yet
            null, // receiptPath - not stored in SQLite yet
            null  // bankTransactionId - not stored in SQLite yet
        );
    }

    // === Business Operations ===

    /**
     * Ensures a business record exists for the given ID.
     * Required for FK constraints.
     */
    public synchronized void ensureBusinessExists(UUID businessId) {
        String sql = "INSERT OR IGNORE INTO business (id) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to ensure business exists: " + businessId, e);
        }
    }

    // === Query Methods ===

    /**
     * Finds expenses by business ID.
     */
    public synchronized List<Expense> findExpensesByBusinessId(UUID businessId) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM expenses WHERE business_id = ? ORDER BY date DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                expenses.add(mapExpense(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find expenses by business ID", e);
        }
        return expenses;
    }

    /**
     * Finds expenses by date range for a business.
     */
    public synchronized List<Expense> findExpensesByDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM expenses WHERE business_id = ? AND date >= ? AND date <= ? ORDER BY date DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                expenses.add(mapExpense(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find expenses by date range", e);
        }
        return expenses;
    }

    /**
     * Finds income by business ID.
     */
    public synchronized List<Income> findIncomeByBusinessId(UUID businessId) {
        List<Income> incomeList = new ArrayList<>();
        String sql = "SELECT * FROM income WHERE business_id = ? ORDER BY date DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                incomeList.add(mapIncome(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find income by business ID", e);
        }
        return incomeList;
    }

    /**
     * Finds income by date range for a business.
     */
    public synchronized List<Income> findIncomeByDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        List<Income> incomeList = new ArrayList<>();
        String sql = "SELECT * FROM income WHERE business_id = ? AND date >= ? AND date <= ? ORDER BY date DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                incomeList.add(mapIncome(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find income by date range", e);
        }
        return incomeList;
    }

    // === Aggregation Methods ===

    /**
     * Calculates total expenses for a business within a date range.
     */
    public synchronized BigDecimal calculateTotalExpenses(UUID businessId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(CAST(amount AS DECIMAL)), 0) FROM expenses WHERE business_id = ? AND date >= ? AND date <= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new BigDecimal(rs.getString(1));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to calculate total expenses", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculates allowable expenses for a business within a date range.
     */
    public synchronized BigDecimal calculateAllowableExpenses(UUID businessId, LocalDate startDate, LocalDate endDate) {
        // Get allowable categories
        List<String> allowableCategories = Arrays.stream(ExpenseCategory.values())
                .filter(ExpenseCategory::isAllowable)
                .map(Enum::name)
                .toList();

        if (allowableCategories.isEmpty()) {
            return BigDecimal.ZERO;
        }

        String placeholders = String.join(",", allowableCategories.stream().map(c -> "?").toList());
        String sql = "SELECT COALESCE(SUM(CAST(amount AS DECIMAL)), 0) FROM expenses " +
                "WHERE business_id = ? AND date >= ? AND date <= ? AND category IN (" + placeholders + ")";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            int idx = 4;
            for (String category : allowableCategories) {
                pstmt.setString(idx++, category);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new BigDecimal(rs.getString(1));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to calculate allowable expenses", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculates total income for a business within a date range.
     */
    public synchronized BigDecimal calculateTotalIncome(UUID businessId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(CAST(amount AS DECIMAL)), 0) FROM income WHERE business_id = ? AND date >= ? AND date <= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new BigDecimal(rs.getString(1));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to calculate total income", e);
        }
        return BigDecimal.ZERO;
    }

    // === Transaction Support ===

    /**
     * Executes a runnable within a transaction.
     * Rolls back on any exception.
     */
    public synchronized boolean executeInTransaction(Runnable action) {
        try {
            connection.setAutoCommit(false);
            action.run();
            connection.commit();
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Transaction failed, rolling back", e);
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                LOG.log(Level.SEVERE, "Rollback failed", rollbackEx);
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Failed to restore auto-commit", e);
            }
        }
    }

    // === Diagnostic Methods ===

    /**
     * Returns true if using in-memory database.
     */
    public boolean isInMemory() {
        return inMemory;
    }

    /**
     * Returns the current journal mode.
     */
    public synchronized String getJournalMode() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to get journal mode", e);
        }
        return "unknown";
    }

    /**
     * Returns true if foreign keys are enabled.
     */
    public synchronized boolean areForeignKeysEnabled() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys")) {
            if (rs.next()) {
                return rs.getInt(1) == 1;
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to check foreign keys", e);
        }
        return false;
    }

    // === Utility Methods ===

    /**
     * Returns the database file path.
     */
    public Path getDatabasePath() {
        return databasePath;
    }

    /**
     * Closes the database connection.
     */
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("SQLite connection closed");
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error closing SQLite connection", e);
        }
    }

    /**
     * Returns the count of expenses.
     */
    public synchronized long countExpenses() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM expenses")) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to count expenses", e);
        }
        return 0;
    }

    /**
     * Returns the count of income entries.
     */
    public synchronized long countIncome() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM income")) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to count income", e);
        }
        return 0;
    }

    // === Terms Acceptance Operations (SE-508) ===

    /**
     * Saves a Terms of Service acceptance.
     *
     * @param tosVersion          The version of the ToS being accepted
     * @param acceptedAt          The timestamp of acceptance (UTC)
     * @param scrollCompletedAt   The timestamp when user scrolled to bottom (UTC)
     * @param applicationVersion  The version of the application
     * @return true if saved successfully, false otherwise
     */
    public synchronized boolean saveTermsAcceptance(String tosVersion, java.time.Instant acceptedAt,
                                                     java.time.Instant scrollCompletedAt, String applicationVersion) {
        String sql = """
            INSERT INTO terms_acceptance (id, tos_version, accepted_at, scroll_completed_at, application_version)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, tosVersion);
            pstmt.setString(3, acceptedAt.toString());
            pstmt.setString(4, scrollCompletedAt.toString());
            pstmt.setString(5, applicationVersion);
            pstmt.executeUpdate();
            LOG.info("Saved Terms acceptance for version: " + tosVersion);
            return true;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save Terms acceptance", e);
            return false;
        }
    }

    /**
     * Gets the most recently accepted ToS version.
     *
     * @return Optional containing the version string, or empty if no acceptances exist
     */
    public synchronized Optional<String> getLatestAcceptedTermsVersion() {
        String sql = "SELECT tos_version FROM terms_acceptance ORDER BY accepted_at DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return Optional.of(rs.getString("tos_version"));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to get latest Terms version", e);
        }
        return Optional.empty();
    }

    /**
     * Gets the timestamp of the most recent Terms acceptance.
     *
     * @return Optional containing the timestamp, or empty if no acceptances exist
     */
    public synchronized Optional<java.time.Instant> getLatestTermsAcceptanceTimestamp() {
        String sql = "SELECT accepted_at FROM terms_acceptance ORDER BY accepted_at DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return Optional.of(java.time.Instant.parse(rs.getString("accepted_at")));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to get Terms acceptance timestamp", e);
        }
        return Optional.empty();
    }

    /**
     * Gets the scroll completed timestamp of the most recent Terms acceptance.
     *
     * @return Optional containing the timestamp, or empty if no acceptances exist
     */
    public synchronized Optional<java.time.Instant> getLatestTermsScrollCompletedTimestamp() {
        String sql = "SELECT scroll_completed_at FROM terms_acceptance ORDER BY accepted_at DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return Optional.of(java.time.Instant.parse(rs.getString("scroll_completed_at")));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to get Terms scroll completed timestamp", e);
        }
        return Optional.empty();
    }

    // === Privacy Acknowledgment Operations (SE-507) ===

    /**
     * Saves a privacy notice acknowledgment.
     *
     * @param privacyVersion      The version of the privacy notice being acknowledged
     * @param acknowledgedAt      The timestamp of acknowledgment (UTC)
     * @param applicationVersion  The version of the application
     * @return true if saved successfully, false otherwise
     */
    public synchronized boolean savePrivacyAcknowledgment(String privacyVersion, java.time.Instant acknowledgedAt,
                                                           String applicationVersion) {
        String sql = """
            INSERT INTO privacy_acknowledgment (id, privacy_version, acknowledged_at, application_version)
            VALUES (?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, privacyVersion);
            pstmt.setString(3, acknowledgedAt.toString());
            pstmt.setString(4, applicationVersion);
            pstmt.executeUpdate();
            LOG.info("Saved Privacy acknowledgment for version: " + privacyVersion);
            return true;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save Privacy acknowledgment", e);
            return false;
        }
    }

    /**
     * Gets the most recently acknowledged privacy notice version.
     *
     * @return Optional containing the version string, or empty if no acknowledgments exist
     */
    public synchronized Optional<String> getLatestAcknowledgedPrivacyVersion() {
        String sql = "SELECT privacy_version FROM privacy_acknowledgment ORDER BY acknowledged_at DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return Optional.of(rs.getString("privacy_version"));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to get latest Privacy version", e);
        }
        return Optional.empty();
    }

    /**
     * Gets the timestamp of the most recent Privacy acknowledgment.
     *
     * @return Optional containing the timestamp, or empty if no acknowledgments exist
     */
    public synchronized Optional<java.time.Instant> getLatestPrivacyAcknowledgmentTimestamp() {
        String sql = "SELECT acknowledged_at FROM privacy_acknowledgment ORDER BY acknowledged_at DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return Optional.of(java.time.Instant.parse(rs.getString("acknowledged_at")));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to get Privacy acknowledgment timestamp", e);
        }
        return Optional.empty();
    }

    // === Bank Transaction Operations ===

    /**
     * Saves a bank transaction to the database.
     * Uses INSERT OR REPLACE to handle updates.
     */
    public synchronized void saveBankTransaction(BankTransaction tx) {
        String sql = """
            INSERT OR REPLACE INTO bank_transactions
            (id, business_id, import_audit_id, source_format_id, date, amount,
             description, account_last_four, bank_transaction_id, transaction_hash,
             review_status, income_id, expense_id, exclusion_reason,
             is_business, confidence_score, suggested_category, created_at, updated_at,
             deleted_at, deleted_by, deletion_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tx.id().toString());
            pstmt.setString(2, tx.businessId().toString());
            pstmt.setString(3, tx.importAuditId().toString());
            pstmt.setString(4, tx.sourceFormatId());
            pstmt.setString(5, tx.date().toString());
            pstmt.setString(6, tx.amount().toPlainString());
            pstmt.setString(7, tx.description());
            pstmt.setString(8, tx.accountLastFour());
            pstmt.setString(9, tx.bankTransactionId());
            pstmt.setString(10, tx.transactionHash());
            pstmt.setString(11, tx.reviewStatus().name());
            pstmt.setString(12, tx.incomeId() != null ? tx.incomeId().toString() : null);
            pstmt.setString(13, tx.expenseId() != null ? tx.expenseId().toString() : null);
            pstmt.setString(14, tx.exclusionReason());
            if (tx.isBusiness() != null) {
                pstmt.setInt(15, tx.isBusiness() ? 1 : 0);
            } else {
                pstmt.setNull(15, Types.INTEGER);
            }
            pstmt.setString(16, tx.confidenceScore() != null ? tx.confidenceScore().toPlainString() : null);
            pstmt.setString(17, tx.suggestedCategory() != null ? tx.suggestedCategory().name() : null);
            pstmt.setString(18, tx.createdAt().toString());
            pstmt.setString(19, tx.updatedAt() != null ? tx.updatedAt().toString() : tx.createdAt().toString());
            pstmt.setString(20, tx.deletedAt() != null ? tx.deletedAt().toString() : null);
            pstmt.setString(21, tx.deletedBy());
            pstmt.setString(22, tx.deletionReason());
            pstmt.executeUpdate();
            LOG.fine("Saved bank transaction: " + tx.id());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save bank transaction: " + tx.id(), e);
        }
    }

    /**
     * Finds all bank transactions for a business, ordered by date descending.
     */
    public synchronized List<BankTransaction> findBankTransactions(UUID businessId) {
        List<BankTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM bank_transactions WHERE business_id = ? AND deleted_at IS NULL ORDER BY date DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                transactions.add(mapBankTransaction(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find bank transactions", e);
        }
        return transactions;
    }

    /**
     * Finds a bank transaction by ID.
     */
    public synchronized Optional<BankTransaction> findBankTransactionById(UUID id) {
        String sql = "SELECT * FROM bank_transactions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapBankTransaction(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find bank transaction: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Counts bank transactions by review status for a business.
     */
    public synchronized long countBankTransactionsByStatus(UUID businessId, String status) {
        String sql = "SELECT COUNT(*) FROM bank_transactions WHERE business_id = ? AND review_status = ? AND deleted_at IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, status);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to count bank transactions by status", e);
        }
        return 0;
    }

    /**
     * Counts all bank transactions for a business.
     */
    public synchronized long countBankTransactions(UUID businessId) {
        String sql = "SELECT COUNT(*) FROM bank_transactions WHERE business_id = ? AND deleted_at IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to count bank transactions", e);
        }
        return 0;
    }

    /**
     * Checks if a bank transaction with the given hash exists for a business.
     */
    public synchronized boolean existsByTransactionHash(UUID businessId, String hash) {
        String sql = "SELECT COUNT(*) FROM bank_transactions WHERE business_id = ? AND transaction_hash = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, hash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to check transaction hash", e);
        }
        return false;
    }

    /**
     * Soft-deletes a bank transaction by ID.
     * Sets the deleted_at timestamp instead of removing the row,
     * preserving data for the 6-year HMRC retention period (TMA 1970 s.12B).
     */
    public synchronized boolean deleteBankTransaction(UUID id) {
        String sql = "UPDATE bank_transactions SET deleted_at = ?, deleted_by = ?, deletion_reason = ? WHERE id = ? AND deleted_at IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, Instant.now().toString());
            pstmt.setString(2, "local-user");
            pstmt.setString(3, "User-initiated deletion");
            pstmt.setString(4, id.toString());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to soft-delete bank transaction: " + id, e);
            return false;
        }
    }

    /**
     * Records a modification to a bank transaction in the audit log.
     * This creates an immutable record of every state change for MTD compliance.
     *
     * @param bankTransactionId the transaction being modified
     * @param modificationType  one of: CATEGORIZED, EXCLUDED, RECATEGORIZED, RESTORED,
     *                          BUSINESS_PERSONAL_CHANGED, CATEGORY_CHANGED
     * @param fieldName         the field that was changed (e.g. "review_status", "is_business")
     * @param previousValue     the value before the change (null if not applicable)
     * @param newValue          the value after the change
     * @param modifiedBy        who made the change (e.g. "local-user")
     */
    public synchronized void logTransactionModification(
            UUID bankTransactionId, String modificationType,
            String fieldName, String previousValue, String newValue,
            String modifiedBy) {
        String sql = """
            INSERT INTO transaction_modification_log
            (id, bank_transaction_id, modification_type, field_name,
             previous_value, new_value, modified_by, modified_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, bankTransactionId.toString());
            pstmt.setString(3, modificationType);
            pstmt.setString(4, fieldName);
            pstmt.setString(5, previousValue);
            pstmt.setString(6, newValue);
            pstmt.setString(7, modifiedBy);
            pstmt.setString(8, Instant.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to log transaction modification for: " + bankTransactionId, e);
        }
    }

    /**
     * Finds all modification log entries for a bank transaction, ordered by time ascending.
     *
     * @param bankTransactionId the transaction ID to look up
     * @return list of log entries as field-value maps
     */
    public synchronized List<Map<String, String>> findModificationLogs(UUID bankTransactionId) {
        List<Map<String, String>> logs = new ArrayList<>();
        String sql = "SELECT * FROM transaction_modification_log WHERE bank_transaction_id = ? ORDER BY modified_at ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bankTransactionId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> entry = new HashMap<>();
                entry.put("id", rs.getString("id"));
                entry.put("bank_transaction_id", rs.getString("bank_transaction_id"));
                entry.put("modification_type", rs.getString("modification_type"));
                entry.put("field_name", rs.getString("field_name"));
                entry.put("previous_value", rs.getString("previous_value"));
                entry.put("new_value", rs.getString("new_value"));
                entry.put("modified_by", rs.getString("modified_by"));
                entry.put("modified_at", rs.getString("modified_at"));
                logs.add(entry);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find modification logs for: " + bankTransactionId, e);
        }
        return logs;
    }

    private BankTransaction mapBankTransaction(ResultSet rs) throws SQLException {
        String incomeIdStr = rs.getString("income_id");
        String expenseIdStr = rs.getString("expense_id");
        String confidenceStr = rs.getString("confidence_score");
        String categoryStr = rs.getString("suggested_category");
        String updatedAtStr = rs.getString("updated_at");
        String deletedAtStr = rs.getString("deleted_at");

        // Handle nullable is_business column
        int isBusinessInt = rs.getInt("is_business");
        Boolean isBusiness = rs.wasNull() ? null : (isBusinessInt == 1);

        return new BankTransaction(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("business_id")),
            UUID.fromString(rs.getString("import_audit_id")),
            rs.getString("source_format_id"),
            LocalDate.parse(rs.getString("date")),
            new BigDecimal(rs.getString("amount")),
            rs.getString("description"),
            rs.getString("account_last_four"),
            rs.getString("bank_transaction_id"),
            rs.getString("transaction_hash"),
            ReviewStatus.valueOf(rs.getString("review_status")),
            incomeIdStr != null ? UUID.fromString(incomeIdStr) : null,
            expenseIdStr != null ? UUID.fromString(expenseIdStr) : null,
            rs.getString("exclusion_reason"),
            isBusiness,
            confidenceStr != null ? new BigDecimal(confidenceStr) : null,
            categoryStr != null ? ExpenseCategory.valueOf(categoryStr) : null,
            Instant.parse(rs.getString("created_at")),
            updatedAtStr != null ? Instant.parse(updatedAtStr) : null,
            deletedAtStr != null ? Instant.parse(deletedAtStr) : null,
            rs.getString("deleted_by"),
            rs.getString("deletion_reason")
        );
    }

    // === Submission Operations (BUG-10H-001) ===

    /**
     * Saves a submission record to the database.
     * Uses INSERT OR REPLACE to handle updates.
     *
     * @param submission The submission record to save
     */
    public synchronized void saveSubmission(SubmissionRecord submission) {
        String sql = """
            INSERT OR REPLACE INTO submissions
            (id, business_id, type, tax_year_start, period_start, period_end,
             total_income, total_expenses, net_profit, status, hmrc_reference,
             error_message, submitted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, submission.id());
            pstmt.setString(2, submission.businessId());
            pstmt.setString(3, submission.type());
            pstmt.setInt(4, submission.taxYearStart());
            pstmt.setString(5, submission.periodStart().toString());
            pstmt.setString(6, submission.periodEnd().toString());
            pstmt.setString(7, submission.totalIncome().toPlainString());
            pstmt.setString(8, submission.totalExpenses().toPlainString());
            pstmt.setString(9, submission.netProfit().toPlainString());
            pstmt.setString(10, submission.status());
            pstmt.setString(11, submission.hmrcReference());
            pstmt.setString(12, submission.errorMessage());
            pstmt.setString(13, submission.submittedAt().toString());
            pstmt.executeUpdate();
            LOG.fine("Saved submission: " + submission.id());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save submission: " + submission.id(), e);
        }
    }

    /**
     * Finds all submissions for a business, ordered by submitted_at descending.
     *
     * @param businessId The business ID
     * @return List of submissions, newest first
     */
    public synchronized List<SubmissionRecord> findSubmissionsByBusinessId(UUID businessId) {
        List<SubmissionRecord> submissions = new ArrayList<>();
        String sql = "SELECT * FROM submissions WHERE business_id = ? ORDER BY submitted_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                submissions.add(mapSubmission(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find submissions by business ID", e);
        }
        return submissions;
    }

    /**
     * Finds a submission by ID.
     *
     * @param id The submission ID
     * @return The submission if found
     */
    public synchronized Optional<SubmissionRecord> findSubmissionById(String id) {
        String sql = "SELECT * FROM submissions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapSubmission(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find submission: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Finds submissions by tax year for a business.
     *
     * @param businessId   The business ID
     * @param taxYearStart The tax year start (e.g., 2025 for 2025/26)
     * @return List of submissions for the tax year
     */
    public synchronized List<SubmissionRecord> findSubmissionsByTaxYear(UUID businessId, int taxYearStart) {
        List<SubmissionRecord> submissions = new ArrayList<>();
        String sql = "SELECT * FROM submissions WHERE business_id = ? AND tax_year_start = ? ORDER BY submitted_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.setInt(2, taxYearStart);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                submissions.add(mapSubmission(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find submissions by tax year", e);
        }
        return submissions;
    }

    /**
     * Deletes a submission by ID.
     *
     * @param id The submission ID
     * @return true if deleted, false if not found
     */
    public synchronized boolean deleteSubmission(String id) {
        String sql = "DELETE FROM submissions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to delete submission: " + id, e);
            return false;
        }
    }

    /**
     * Counts submissions for a business.
     *
     * @param businessId The business ID
     * @return The count of submissions
     */
    public synchronized long countSubmissions(UUID businessId) {
        String sql = "SELECT COUNT(*) FROM submissions WHERE business_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to count submissions", e);
        }
        return 0;
    }

    /**
     * Maps a ResultSet row to a SubmissionRecord.
     */
    private SubmissionRecord mapSubmission(ResultSet rs) throws SQLException {
        return new SubmissionRecord(
            rs.getString("id"),
            rs.getString("business_id"),
            rs.getString("type"),
            rs.getInt("tax_year_start"),
            LocalDate.parse(rs.getString("period_start")),
            LocalDate.parse(rs.getString("period_end")),
            new BigDecimal(rs.getString("total_income")),
            new BigDecimal(rs.getString("total_expenses")),
            new BigDecimal(rs.getString("net_profit")),
            rs.getString("status"),
            rs.getString("hmrc_reference"),
            rs.getString("error_message"),
            Instant.parse(rs.getString("submitted_at"))
        );
    }

    // === Reconciliation Match Operations ===

    /**
     * Saves a reconciliation match to the database.
     * Uses INSERT OR REPLACE to handle updates and unique constraint conflicts.
     *
     * @param match the reconciliation match to save
     */
    public synchronized void saveReconciliationMatch(ReconciliationMatch match) {
        String sql = """
            INSERT OR REPLACE INTO reconciliation_matches
            (id, bank_transaction_id, manual_transaction_id, manual_transaction_type,
             confidence, match_tier, status, business_id, created_at, resolved_at, resolved_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, match.id().toString());
            pstmt.setString(2, match.bankTransactionId().toString());
            pstmt.setString(3, match.manualTransactionId().toString());
            pstmt.setString(4, match.manualTransactionType());
            pstmt.setDouble(5, match.confidence());
            pstmt.setString(6, match.matchTier().name());
            pstmt.setString(7, match.status().name());
            pstmt.setString(8, match.businessId().toString());
            pstmt.setString(9, match.createdAt().toString());
            pstmt.setString(10, match.resolvedAt() != null ? match.resolvedAt().toString() : null);
            pstmt.setString(11, match.resolvedBy());
            pstmt.executeUpdate();
            LOG.fine("Saved reconciliation match: " + match.id());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save reconciliation match: " + match.id(), e);
        }
    }

    /**
     * Saves multiple reconciliation matches in a single transaction.
     *
     * @param matches the list of reconciliation matches to save
     */
    public synchronized void saveReconciliationMatches(List<ReconciliationMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return;
        }
        executeInTransaction(() -> {
            for (ReconciliationMatch match : matches) {
                saveReconciliationMatch(match);
            }
        });
    }

    /**
     * Finds a reconciliation match by ID.
     *
     * @param id the match ID
     * @return the match if found
     */
    public synchronized Optional<ReconciliationMatch> findReconciliationMatchById(UUID id) {
        String sql = "SELECT * FROM reconciliation_matches WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapReconciliationMatch(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to find reconciliation match: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Finds all reconciliation matches for a bank transaction.
     *
     * @param bankTransactionId the bank transaction ID
     * @return list of matches for that bank transaction
     */
    public synchronized List<ReconciliationMatch> findReconciliationMatchesByBankTransactionId(UUID bankTransactionId) {
        List<ReconciliationMatch> matches = new ArrayList<>();
        String sql = "SELECT * FROM reconciliation_matches WHERE bank_transaction_id = ? ORDER BY confidence DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bankTransactionId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapReconciliationMatch(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find reconciliation matches by bank tx: " + bankTransactionId, e);
        }
        return matches;
    }

    /**
     * Finds all reconciliation matches for a business.
     *
     * @param businessId the business ID
     * @return list of all matches for that business
     */
    public synchronized List<ReconciliationMatch> findReconciliationMatchesByBusinessId(UUID businessId) {
        List<ReconciliationMatch> matches = new ArrayList<>();
        String sql = "SELECT * FROM reconciliation_matches WHERE business_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapReconciliationMatch(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find reconciliation matches by business: " + businessId, e);
        }
        return matches;
    }

    /**
     * Finds all unresolved reconciliation matches for a business.
     *
     * @param businessId the business ID
     * @return list of unresolved matches
     */
    public synchronized List<ReconciliationMatch> findUnresolvedReconciliationMatches(UUID businessId) {
        List<ReconciliationMatch> matches = new ArrayList<>();
        String sql = "SELECT * FROM reconciliation_matches WHERE business_id = ? AND status = 'UNRESOLVED' ORDER BY confidence DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(mapReconciliationMatch(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find unresolved reconciliation matches", e);
        }
        return matches;
    }

    /**
     * Counts unresolved reconciliation matches for a business.
     *
     * @param businessId the business ID
     * @return count of unresolved matches
     */
    public synchronized long countUnresolvedReconciliationMatches(UUID businessId) {
        String sql = "SELECT COUNT(*) FROM reconciliation_matches WHERE business_id = ? AND status = 'UNRESOLVED'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to count unresolved reconciliation matches", e);
        }
        return 0;
    }

    /**
     * Updates the status of a reconciliation match.
     * This is the only modification allowed on reconciliation records
     * (they must never be hard-deleted per statutory requirements).
     *
     * @param matchId    the match ID
     * @param status     the new status
     * @param resolvedAt when the status was changed
     * @param resolvedBy who changed the status
     * @return true if updated, false if not found
     */
    public synchronized boolean updateReconciliationMatchStatus(
            UUID matchId, ReconciliationStatus status, Instant resolvedAt, String resolvedBy) {
        String sql = "UPDATE reconciliation_matches SET status = ?, resolved_at = ?, resolved_by = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, resolvedAt != null ? resolvedAt.toString() : null);
            pstmt.setString(3, resolvedBy);
            pstmt.setString(4, matchId.toString());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to update reconciliation match status: " + matchId, e);
            return false;
        }
    }

    /**
     * Maps a ResultSet row to a ReconciliationMatch.
     */
    private ReconciliationMatch mapReconciliationMatch(ResultSet rs) throws SQLException {
        String resolvedAtStr = rs.getString("resolved_at");
        return new ReconciliationMatch(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("bank_transaction_id")),
            UUID.fromString(rs.getString("manual_transaction_id")),
            rs.getString("manual_transaction_type"),
            rs.getDouble("confidence"),
            MatchTier.valueOf(rs.getString("match_tier")),
            ReconciliationStatus.valueOf(rs.getString("status")),
            UUID.fromString(rs.getString("business_id")),
            Instant.parse(rs.getString("created_at")),
            resolvedAtStr != null ? Instant.parse(resolvedAtStr) : null,
            rs.getString("resolved_by")
        );
    }
}
