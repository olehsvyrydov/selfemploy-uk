package uk.selfemploy.ui.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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

    /**
     * The primary connection: used for schema init/migration, and the sole connection in in-memory
     * (test) mode. In file mode it also serves the thread that constructed the store.
     */
    private Connection connection;

    /**
     * File mode only: each thread gets its own connection so concurrent threads (e.g. a background
     * CSV-import thread writing while the JavaFX thread reads) never share one non-thread-safe
     * {@link Connection}. In-memory mode keeps the single shared {@link #connection}, since a
     * {@code :memory:} database is private to its connection.
     */
    private final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

    /** Every connection handed out, tracked so {@link #close()} can release them all. */
    private final List<Connection> openConnections = new CopyOnWriteArrayList<>();

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
            migrateSchema();

            if (!inMemory) {
                // The constructing thread reuses the primary connection; other threads open their own.
                openConnections.add(connection);
                threadConnection.set(connection);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to initialize SQLite database", e);
        }
    }

    /**
     * Applies schema migrations for existing databases that were created
     * before new columns were added. ALTER TABLE ADD COLUMN is safe to
     * call even if the column already exists (caught and ignored).
     */
    private void migrateSchema() {
        addColumnIfMissing("bank_transactions", "deleted_at", "TEXT");
        addColumnIfMissing("bank_transactions", "deleted_by", "TEXT");
        addColumnIfMissing("bank_transactions", "deletion_reason", "TEXT");
        addColumnIfMissing("income", "client_name", "TEXT");
        addColumnIfMissing("income", "status", "TEXT NOT NULL DEFAULT 'PAID'");
        migrateSubmissionHonesty();
    }

    /**
     * Makes the submission history honest.
     *
     * <p>Older builds fabricated an HMRC annual submission: they wrote rows with an
     * invented "SA-..." reference and a status of ACCEPTED, even though nothing was
     * ever sent to HMRC. This migration widens the status CHECK constraint to allow
     * NOT_SUBMITTED (older on-disk tables were created without it), then relabels
     * every fabricated row as NOT_SUBMITTED and clears its counterfeit reference so
     * the history screen can no longer present it as an accepted HMRC filing.</p>
     *
     * <p>A real HMRC reference is never of the "SA-" form, so the relabel targets
     * exactly the fabricated rows. Both steps are idempotent.</p>
     */
    void migrateSubmissionHonesty() {
        try {
            String ddl = null;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type='table' AND name='submissions'")) {
                if (rs.next()) {
                    ddl = rs.getString(1);
                }
            }
            if (ddl == null) {
                return; // no submissions table yet
            }
            if (!ddl.contains("NOT_SUBMITTED")) {
                rebuildSubmissionsTableWithHonestCheck();
            }
            try (Statement stmt = connection.createStatement()) {
                int relabelled = stmt.executeUpdate(
                    "UPDATE submissions SET status = 'NOT_SUBMITTED', hmrc_reference = NULL "
                    + "WHERE hmrc_reference LIKE 'SA-%'");
                if (relabelled > 0) {
                    LOG.info("Relabelled " + relabelled
                        + " fabricated submission row(s) as NOT_SUBMITTED");
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to migrate submission history to honest state", e);
            throw new RuntimeException("Submission history migration failed", e);
        }
    }

    /**
     * Rebuilds the submissions table so its status CHECK constraint allows
     * NOT_SUBMITTED. SQLite cannot alter a CHECK constraint in place, so the table
     * is recreated and its data copied across.
     */
    private void rebuildSubmissionsTableWithHonestCheck() throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            connection.setAutoCommit(false);
            stmt.execute("""
                CREATE TABLE submissions_new (
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
                    CHECK (status IN ('PENDING', 'SUBMITTED', 'ACCEPTED', 'REJECTED', 'NOT_SUBMITTED'))
                )
            """);
            stmt.execute("""
                INSERT INTO submissions_new
                    (id, business_id, type, tax_year_start, period_start, period_end,
                     total_income, total_expenses, net_profit, status, hmrc_reference,
                     error_message, submitted_at, created_at)
                SELECT id, business_id, type, tax_year_start, period_start, period_end,
                     total_income, total_expenses, net_profit, status, hmrc_reference,
                     error_message, submitted_at, created_at
                FROM submissions
            """);
            stmt.execute("DROP TABLE submissions");
            stmt.execute("ALTER TABLE submissions_new RENAME TO submissions");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_submissions_business ON submissions(business_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_submissions_tax_year ON submissions(tax_year_start)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_submissions_status ON submissions(status)");
            connection.commit();
            LOG.info("Rebuilt submissions table to allow NOT_SUBMITTED status");
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    /**
     * Test-only hook: runs raw DDL/DML against the current connection so migration
     * tests can stage a legacy schema. Not for production use.
     */
    void executeRawForTest(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void addColumnIfMissing(String table, String column, String type) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            LOG.info("Added missing column " + table + "." + column);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("duplicate column")) {
                LOG.fine("Column " + table + "." + column + " already exists");
            } else {
                LOG.log(Level.SEVERE, "Failed to add column " + table + "." + column, e);
                throw new RuntimeException("Schema migration failed: " + table + "." + column, e);
            }
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
                    client_name TEXT,
                    status TEXT NOT NULL DEFAULT 'PAID',
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
                    CHECK (status IN ('PENDING', 'SUBMITTED', 'ACCEPTED', 'REJECTED', 'NOT_SUBMITTED'))
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
            try (PreparedStatement pstmt = connection().prepareStatement(sql)) {
                pstmt.setString(1, key);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Failed to delete setting: " + key, e);
            }
        } else {
            String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection().prepareStatement(sql)) {
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
        try (PreparedStatement pstmt = connection().prepareStatement(sql)) {
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

    // === HMRC API Credential Operations ===

    private static final CredentialEncryption credentialEncryption = new CredentialEncryption();

    /**
     * Saves the HMRC API client ID, encrypted at rest.
     *
     * @param clientId the HMRC Developer Hub client ID, or null to clear
     */
    public synchronized void saveHmrcClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            saveSetting("hmrc_client_id_enc", null);
        } else {
            saveSetting("hmrc_client_id_enc", credentialEncryption.encrypt(clientId));
        }
    }

    /**
     * Loads the stored HMRC API client ID.
     *
     * @return the decrypted client ID, or null if not set
     */
    public synchronized String loadHmrcClientId() {
        String encrypted = loadSetting("hmrc_client_id_enc");
        if (encrypted == null) {
            return null;
        }
        try {
            return credentialEncryption.decrypt(encrypted);
        } catch (CredentialEncryptionException e) {
            LOG.log(Level.WARNING, "Failed to decrypt HMRC client ID - clearing corrupted value", e);
            saveSetting("hmrc_client_id_enc", null);
            return null;
        }
    }

    /**
     * Saves the HMRC API client secret, encrypted at rest.
     *
     * @param clientSecret the HMRC Developer Hub client secret, or null to clear
     */
    public synchronized void saveHmrcClientSecret(String clientSecret) {
        if (clientSecret == null || clientSecret.isBlank()) {
            saveSetting("hmrc_client_secret_enc", null);
        } else {
            saveSetting("hmrc_client_secret_enc", credentialEncryption.encrypt(clientSecret));
        }
    }

    /**
     * Loads the stored HMRC API client secret.
     *
     * @return the decrypted client secret, or null if not set
     */
    public synchronized String loadHmrcClientSecret() {
        String encrypted = loadSetting("hmrc_client_secret_enc");
        if (encrypted == null) {
            return null;
        }
        try {
            return credentialEncryption.decrypt(encrypted);
        } catch (CredentialEncryptionException e) {
            LOG.log(Level.WARNING, "Failed to decrypt HMRC client secret - clearing corrupted value", e);
            saveSetting("hmrc_client_secret_enc", null);
            return null;
        }
    }

    /**
     * Checks if HMRC API credentials are stored.
     *
     * @return true if both client ID and client secret are stored
     */
    public synchronized boolean hasHmrcCredentials() {
        return loadHmrcClientId() != null && loadHmrcClientSecret() != null;
    }

    /**
     * Clears all stored HMRC API credentials.
     */
    public synchronized void clearHmrcCredentials() {
        saveSetting("hmrc_client_id_enc", null);
        saveSetting("hmrc_client_secret_enc", null);
    }

    // === HMRC Environment ===

    /**
     * Saves the HMRC environment setting ("sandbox" or "production").
     * Null or blank values default to "sandbox".
     */
    public synchronized void saveHmrcEnvironment(String environment) {
        if (environment == null || environment.isBlank()) {
            saveSetting("hmrc_environment", "sandbox");
        } else {
            saveSetting("hmrc_environment", environment.trim().toLowerCase());
        }
    }

    /**
     * Loads the HMRC environment setting. Defaults to "sandbox" if not set.
     */
    public synchronized String loadHmrcEnvironment() {
        String env = loadSetting("hmrc_environment");
        return env == null ? "sandbox" : env;
    }

    /**
     * Returns true if the current environment is sandbox (the default).
     */
    public synchronized boolean isSandboxEnvironment() {
        return "sandbox".equals(loadHmrcEnvironment());
    }

    // === Business Operations ===

    /**
     * Ensures a business record exists for the given ID.
     * Required for FK constraints.
     */
    public synchronized void ensureBusinessExists(UUID businessId) {
        String sql = "INSERT OR IGNORE INTO business (id) VALUES (?)";
        try (PreparedStatement pstmt = connection().prepareStatement(sql)) {
            pstmt.setString(1, businessId.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to ensure business exists: " + businessId, e);
        }
    }

    // === Transaction Support ===

    /**
     * Executes a runnable within a transaction.
     * Rolls back on any exception.
     */
    public synchronized boolean executeInTransaction(Runnable action) {
        Connection conn = connection();
        try {
            conn.setAutoCommit(false);
            action.run();
            conn.commit();
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Transaction failed, rolling back", e);
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                LOG.log(Level.SEVERE, "Rollback failed", rollbackEx);
            }
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
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
        try (Statement stmt = connection().createStatement();
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
        try (Statement stmt = connection().createStatement();
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
     * Closes every database connection (the primary and every per-thread connection in file mode).
     */
    public synchronized void close() {
        if (inMemory) {
            closeQuietly(connection);
        } else {
            for (Connection conn : openConnections) {
                closeQuietly(conn);
            }
            openConnections.clear();
        }
        LOG.info("SQLite connection(s) closed");
    }

    private void closeQuietly(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error closing SQLite connection", e);
        }
    }

    /**
     * Returns the SQLite connection the calling thread should use. In file mode each thread gets
     * its own connection (opened lazily) so concurrent threads never issue statements on a shared,
     * non-thread-safe {@link Connection}; WAL mode plus {@code busy_timeout} (set on every
     * connection) handle reader/writer concurrency across those connections. In-memory mode returns
     * the single shared connection, because a {@code :memory:} database is private per connection.
     *
     * <p>Package-private so collaborating repositories in this package can reach it.</p>
     *
     * @return the connection for this thread, or null if the store failed to initialise
     */
    synchronized Connection connection() {
        if (inMemory || connection == null) {
            return connection;
        }
        Connection conn = threadConnection.get();
        if (conn == null || isClosedQuietly(conn)) {
            conn = openThreadConnection();
            threadConnection.set(conn);
            openConnections.add(conn);
        }
        return conn;
    }

    /**
     * Opens a fresh file-mode connection for the calling thread and applies the per-connection
     * pragmas. WAL is a persistent database-level setting established on the primary connection, so
     * it is not re-issued here. Falls back to the shared primary connection if opening fails.
     */
    private Connection openThreadConnection() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA busy_timeout = 5000");
            }
            return conn;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to open per-thread SQLite connection; using shared connection", e);
            return connection;
        }
    }

    private static boolean isClosedQuietly(Connection conn) {
        try {
            return conn.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

}
