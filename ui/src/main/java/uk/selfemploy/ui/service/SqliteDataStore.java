package uk.selfemploy.ui.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import uk.selfemploy.ui.service.db.SqliteMigrationRunner;

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
    private final CredentialEncryption credentialEncryption;

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
        this.credentialEncryption = new CredentialEncryption();
        this.inMemory = inMemory;
        this.databasePath = inMemory ? null : resolveDatabasePath();
        if (!inMemory) {
            ensureDirectoryExists();
        }
        initializeDatabase();
        if (!inMemory) {
            restrictDatabaseFiles();
        }
    }

    /**
     * Test seam: a file-mode store backed by an explicit path (bypasses the OS-specific
     * {@link #resolveDatabasePath()}), so tests can exercise the real per-thread-connection path
     * against a temporary database file.
     */
    SqliteDataStore(Path databasePath) {
        this(databasePath, new CredentialEncryption());
    }

    /**
     * Test seam: a file-mode store with an explicit {@link CredentialEncryption}, so tests can bind
     * the at-rest encryption to a temporary master key instead of the real per-user key file.
     */
    SqliteDataStore(Path databasePath, CredentialEncryption credentialEncryption) {
        if (databasePath == null) {
            throw new IllegalArgumentException("databasePath cannot be null");
        }
        this.credentialEncryption = credentialEncryption;
        this.inMemory = false;
        this.databasePath = databasePath;
        ensureDirectoryExists();
        initializeDatabase();
        restrictDatabaseFiles();
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
        return AppDataDirectory.resolve().resolve(DB_FILE);
    }

    /**
     * Ensures the data directory exists and that neither it nor the database is readable by other
     * users of the machine. The database holds OAuth tokens and taxpayer identifiers, so the
     * permissions are re-applied on every start rather than only at creation — directories created
     * by earlier versions were left at the default umask.
     */
    private void ensureDirectoryExists() {
        Path parent = databasePath.getParent();
        if (parent != null) {
            AppDataDirectory.createRestricted(parent);
        }
        restrictDatabaseFiles();
    }

    private void restrictDatabaseFiles() {
        String name = databasePath.getFileName().toString();
        AppDataDirectory.restrictFile(databasePath);
        AppDataDirectory.restrictFile(databasePath.resolveSibling(name + "-wal"));
        AppDataDirectory.restrictFile(databasePath.resolveSibling(name + "-shm"));
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
            new SqliteMigrationRunner(connection).run(migrations());

            if (!inMemory) {
                // The constructing thread reuses the primary connection; other threads open their own.
                openConnections.add(connection);
                threadConnection.set(connection);
            }
        } catch (SQLException | RuntimeException e) {
            // Includes migration failures (e.g. a missing/unreadable migration resource surfaces as a
            // RuntimeException). Fail fast rather than leaving a silently broken schema that would
            // otherwise surface later as confusing "no such table" errors.
            LOG.log(Level.SEVERE, "Failed to initialize SQLite database", e);
            throw new IllegalStateException("Failed to initialize SQLite database", e);
        }
    }

    /**
     * The ordered schema migrations for the desktop store. Applied once each by
     * {@link SqliteMigrationRunner}, which tracks them in the {@code schema_version} ledger.
     * Every migration is idempotent so that an upgrade from a pre-migration database (which has no
     * ledger and thus runs all of them) converges to the same schema as a fresh install.
     */
    private List<SqliteMigrationRunner.Migration> migrations() {
        return List.of(
            SqliteMigrationRunner.script(1, "baseline schema", "/db/migration-sqlite/V1__baseline.sql"),
            SqliteMigrationRunner.java(2, "legacy nullable columns", this::addLegacyColumns),
            SqliteMigrationRunner.java(3, "honest submission history", this::migrateSubmissionHonesty)
        );
    }

    /**
     * Adds columns introduced after the original tables shipped, for databases created before the
     * baseline included them. {@code ALTER TABLE ADD COLUMN} is safe to call when the column already
     * exists (caught and ignored), so this is idempotent.
     */
    private void addLegacyColumns(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "bank_transactions", "deleted_at", "TEXT");
        addColumnIfMissing(conn, "bank_transactions", "deleted_by", "TEXT");
        addColumnIfMissing(conn, "bank_transactions", "deletion_reason", "TEXT");
        addColumnIfMissing(conn, "income", "client_name", "TEXT");
        addColumnIfMissing(conn, "income", "status", "TEXT NOT NULL DEFAULT 'PAID'");
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
        migrateSubmissionHonesty(connection);
    }

    void migrateSubmissionHonesty(Connection conn) {
        try {
            String ddl = null;
            try (Statement stmt = conn.createStatement();
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
                rebuildSubmissionsTableWithHonestCheck(conn);
            }
            try (Statement stmt = conn.createStatement()) {
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
    private void rebuildSubmissionsTableWithHonestCheck(Connection conn) throws SQLException {
        boolean autoCommit = conn.getAutoCommit();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            conn.setAutoCommit(false);
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
            conn.commit();
            LOG.info("Rebuilt submissions table to allow NOT_SUBMITTED status");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
            try (Statement stmt = conn.createStatement()) {
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

    private void addColumnIfMissing(Connection conn, String table, String column, String type) {
        try (Statement stmt = conn.createStatement()) {
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

    // === Onboarding Operations ===

    /**
     * Records whether the first-run onboarding has been completed (or skipped).
     */
    public synchronized void saveOnboardingCompleted(boolean completed) {
        saveSetting("onboarding_completed", completed ? "true" : "false");
    }

    /**
     * Returns true if first-run onboarding has been completed or skipped.
     */
    public synchronized boolean isOnboardingCompleted() {
        return "true".equals(loadSetting("onboarding_completed"));
    }

    /**
     * Saves the tax year chosen during onboarding (e.g. "2025/26"), or null to clear.
     */
    public synchronized void saveOnboardingTaxYear(String taxYear) {
        saveSetting("onboarding_tax_year", taxYear);
    }

    /**
     * Loads the tax year chosen during onboarding, or null if not set.
     */
    public synchronized String loadOnboardingTaxYear() {
        return loadSetting("onboarding_tax_year");
    }

    /**
     * Saves the business type chosen during onboarding (a {@code BusinessType} name), or null to clear.
     */
    public synchronized void saveBusinessType(String businessType) {
        saveSetting("business_type", businessType);
    }

    /**
     * Loads the business type chosen during onboarding, or null if not set.
     */
    public synchronized String loadBusinessType() {
        return loadSetting("business_type");
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
     * Saves OAuth tokens to persistent storage. The access and refresh tokens are encrypted at
     * rest; the refresh token in particular is a long-lived credential for the taxpayer's HMRC
     * account. The remaining fields are not secret and are stored as-is.
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
        saveEncrypted("oauth_access_token", accessToken);
        saveEncrypted("oauth_refresh_token", refreshToken);
        saveSetting("oauth_expires_in", String.valueOf(expiresIn));
        saveSetting("oauth_token_type", tokenType);
        saveSetting("oauth_scope", scope);
        saveSetting("oauth_issued_at", issuedAt != null ? issuedAt.toString() : null);
        LOG.info("OAuth tokens saved to persistent storage");
    }

    /**
     * Persists a value encrypted at rest on a best-effort basis. A key-storage failure is logged
     * and the write is skipped rather than propagated: callers such as {@link #saveOAuthTokens}
     * previously wrote plaintext that could never fail, and upstream token-lifecycle code treats a
     * persistence exception as an expired session and responds by discarding valid tokens. Keeping
     * this non-throwing preserves the in-memory session; the value is re-persisted on the next
     * successful save.
     */
    private void saveEncrypted(String key, String value) {
        if (value == null) {
            saveSetting(key, null);
            return;
        }
        try {
            saveSetting(key, credentialEncryption.encrypt(value));
        } catch (CredentialEncryptionException e) {
            LOG.log(Level.WARNING, "Could not encrypt " + key + " for storage; left prior value", e);
        }
    }

    /**
     * Reads a token that is encrypted at rest. Tokens written by earlier versions are stored in
     * the clear; such a value is rewritten encrypted as soon as it is read, so the plaintext does
     * not survive the upgrade.
     *
     * <p>A value that cannot be decrypted — whether the master key is unavailable or the ciphertext
     * does not decrypt under it — is left in place and reported as absent. The stored value is never
     * deleted on a read: a key that is missing or wrong today may be restored tomorrow, and a
     * genuinely unusable value is harmlessly overwritten by the next save.</p>
     */
    private String loadEncryptedToken(String key) {
        String stored = loadSetting(key);
        if (stored == null) {
            return null;
        }
        if (credentialEncryption.isLegacy(stored)) {
            reEncryptInPlace(key, stored, key);
            return stored;
        }
        try {
            return credentialEncryption.decrypt(stored);
        } catch (CredentialEncryptionException e) {
            LOG.log(Level.WARNING, "Could not decrypt " + key + "; leaving it encrypted at rest", e);
            return null;
        }
    }

    /**
     * Rewrites a plaintext or legacy-scheme value under the current master key. A failure is logged
     * and swallowed rather than propagated or treated as corruption: the value was read correctly,
     * so it must not be cleared just because it could not be rewritten (e.g. the key file is
     * momentarily unwritable); the next load simply retries the rewrite.
     */
    private void reEncryptInPlace(String key, String plaintext, String description) {
        try {
            saveSetting(key, credentialEncryption.encrypt(plaintext));
        } catch (CredentialEncryptionException e) {
            LOG.log(Level.WARNING,
                "Could not re-encrypt " + description + " under the current key; left as stored", e);
        }
    }

    /**
     * Loads OAuth tokens from persistent storage.
     *
     * @return array of [accessToken, refreshToken, expiresIn, tokenType, scope, issuedAt],
     *         or null if not stored
     */
    public synchronized String[] loadOAuthTokens() {
        String accessToken = loadEncryptedToken("oauth_access_token");
        if (accessToken == null) {
            return null;
        }
        return new String[] {
            accessToken,
            loadEncryptedToken("oauth_refresh_token"),
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
        return loadEncryptedCredential("hmrc_client_id_enc", "HMRC client ID");
    }

    /**
     * Reads a credential, rewriting it under the current master key if it was stored under the
     * superseded machine-derived key, so upgrading does not discard the user's credentials.
     */
    private String loadEncryptedCredential(String key, String description) {
        String encrypted = loadSetting(key);
        if (encrypted == null) {
            return null;
        }
        String plaintext;
        try {
            plaintext = credentialEncryption.decrypt(encrypted);
        } catch (CredentialEncryptionException e) {
            // Never delete on a read: a value that will not decrypt today (key missing, wrong, or
            // the ciphertext genuinely unusable) is left intact so a restored key can recover it,
            // and is harmlessly overwritten if the user re-enters it.
            LOG.log(Level.WARNING,
                "Could not decrypt " + description + "; leaving it encrypted at rest", e);
            return null;
        }
        if (credentialEncryption.isLegacy(encrypted)) {
            reEncryptInPlace(key, plaintext, description);
        }
        return plaintext;
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
        return loadEncryptedCredential("hmrc_client_secret_enc", "HMRC client secret");
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
