package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save setting: " + key, e);
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
            rs.getString("notes")
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
            rs.getString("reference")
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
}
