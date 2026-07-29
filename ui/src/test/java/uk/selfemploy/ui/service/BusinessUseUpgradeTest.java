package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.common.domain.TaxYear;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An existing database gains the business-use column without its figures changing.
 *
 * <p>This is the part of adding a column that can hurt someone: the expenses already recorded were
 * entered when there was no such thing as a partial claim, and every one of them meant the whole
 * amount. If the upgrade left them at anything other than wholly business, every allowable total
 * would quietly change on the next launch — including for a year already filed.
 *
 * <p>It builds a database in the old shape rather than mocking one, because the claim is about what
 * happens to a real file.
 */
@DisplayName("Upgrading a database that predates business-use shares")
class BusinessUseUpgradeTest {

    @TempDir
    Path dir;

    private static final UUID BUSINESS = UUID.randomUUID();
    private static final TaxYear TAX_YEAR = TaxYear.of(2025);

    /** The expenses table as it was before the share existed, with two expenses already in it. */
    private void buildDatabaseInTheOldShape(Path database) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE business (
                    id TEXT PRIMARY KEY,
                    name TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')))""");
            s.execute("""
                CREATE TABLE expenses (
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
                    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE)""");
            s.execute("INSERT INTO business (id, name) VALUES ('" + BUSINESS + "', 'Ada')");
            s.execute("INSERT INTO expenses (id, business_id, date, amount, description, category) "
                + "VALUES ('" + UUID.randomUUID() + "', '" + BUSINESS + "', '2025-06-10', '60.00',"
                + " 'Phone bill', 'OFFICE_COSTS')");
            s.execute("INSERT INTO expenses (id, business_id, date, amount, description, category) "
                + "VALUES ('" + UUID.randomUUID() + "', '" + BUSINESS + "', '2025-06-11', '40.00',"
                + " 'Stationery', 'OFFICE_COSTS')");
        }
    }

    @Test
    @DisplayName("expenses recorded before the column existed are wholly business, and total the same")
    void existingExpensesKeepTheirMeaning() throws Exception {
        Path database = dir.resolve("selfemploy.db");
        buildDatabaseInTheOldShape(database);

        SqliteDataStore store = new SqliteDataStore(database);
        try {
            SqliteExpenseRepository expenses = new SqliteExpenseRepository(BUSINESS, store);

            assertThat(expenses.findByTaxYear(TAX_YEAR))
                    .as("both expenses are still there")
                    .hasSize(2)
                    .allSatisfy(expense -> assertThat(expense.businessUsePercentage())
                            .as("an expense entered before shares existed meant all of it")
                            .isEqualTo(100));

            assertThat(expenses.getAllowableTotalByTaxYear(TAX_YEAR))
                    .as("the figure this database reported before the upgrade")
                    .isEqualByComparingTo(new BigDecimal("100.00"));
        } finally {
            store.close();
        }
    }

    @Test
    @DisplayName("the upgrade runs once and can be repeated without harm")
    void upgradingTwiceChangesNothing() throws Exception {
        Path database = dir.resolve("selfemploy.db");
        buildDatabaseInTheOldShape(database);

        new SqliteDataStore(database).close();
        SqliteDataStore reopened = new SqliteDataStore(database);
        try {
            assertThat(new SqliteExpenseRepository(BUSINESS, reopened)
                    .getAllowableTotalByTaxYear(TAX_YEAR))
                    .isEqualByComparingTo(new BigDecimal("100.00"));
        } finally {
            reopened.close();
        }
    }
}
