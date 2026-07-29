package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.service.sql.NamedSql;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * SQLite JDBC adapter for {@link ExpenseRepository}.
 *
 * <p>Owns its own SQL (loaded from {@code /sql/expense.sql}) and its row mapper, running against
 * the shared connection from {@link SqliteDataStore}. The expenses table DDL is handled by
 * {@code SqliteDataStore}'s schema initialisation.</p>
 */
public class SqliteExpenseRepository implements ExpenseRepository {

    private static final Logger LOG = Logger.getLogger(SqliteExpenseRepository.class.getName());

    private static final NamedSql SQL = NamedSql.load("/sql/expense.sql");

    private final SqliteDataStore dataStore;
    private final UUID businessId;

    public SqliteExpenseRepository(UUID businessId) {
        this(businessId, SqliteDataStore.getInstance());
    }

    /**
     * Test seam: binds the repository to an explicit store rather than the singleton, so a real
     * database file can be opened and read back.
     */
    SqliteExpenseRepository(UUID businessId, SqliteDataStore dataStore) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        if (dataStore == null) {
            throw new IllegalArgumentException("Data store cannot be null");
        }
        this.businessId = businessId;
        this.dataStore = dataStore;
        dataStore.ensureBusinessExists(businessId);
    }

    @Override
    public Expense save(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null");
        }
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("insertExpense"))) {
            pstmt.setString(1, expense.id().toString());
            pstmt.setString(2, expense.businessId().toString());
            pstmt.setString(3, expense.date().toString());
            pstmt.setString(4, expense.amount().toPlainString());
            pstmt.setString(5, expense.description());
            pstmt.setString(6, expense.category().name());
            pstmt.setString(7, expense.receiptPath());
            pstmt.setString(8, expense.notes());
            pstmt.setInt(9, expense.businessUsePercentage());
            pstmt.executeUpdate();
            LOG.fine("Saved expense: " + expense.id());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save expense: " + expense.id(), e);
            throw new DataStoreException("Failed to save expense", e);
        }
        return expense;
    }

    @Override
    public Optional<Expense> findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Expense ID cannot be null");
        }
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("findExpenseById"))) {
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

    @Override
    public List<Expense> findAll() {
        List<Expense> expenses = new ArrayList<>();
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("findExpensesByBusiness"))) {
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

    @Override
    public List<Expense> findByTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return findByDateRange(taxYear.startDate(), taxYear.endDate());
    }

    @Override
    public List<Expense> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        List<Expense> expenses = new ArrayList<>();
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findExpensesByBusinessAndDateRange"))) {
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

    @Override
    public List<Expense> findByCategory(ExpenseCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        return findAll().stream()
            .filter(e -> e.category() == category)
            .collect(Collectors.toList());
    }

    /**
     * Adds up an amount-per-row result exactly.
     *
     * <p>The amounts are stored as text because SQLite has no decimal type. Letting SQLite add them
     * up coerces them to floating point, where three amounts of 10.10 come to 30.299999999999997 and
     * the error grows with the number of records. These are the figures a tax return is built from,
     * so they are added in {@link BigDecimal}.
     */
    private static BigDecimal sumAmounts(PreparedStatement pstmt) throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String amount = rs.getString(1);
                if (amount != null && !amount.isBlank()) {
                    total = total.add(new BigDecimal(amount));
                }
            }
        }
        return total;
    }

    /**
     * Adds up the claimable share of each row: its amount times its business-use percentage.
     *
     * <p>Each share is rounded to the penny before being added, so the total is the sum of the
     * figures shown against the individual expenses. Apportioning the total instead would give a
     * number that does not match its own breakdown.
     */
    private BigDecimal sumBusinessUseShares(PreparedStatement pstmt) throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                total = total.add(mapExpense(rs).allowableAmount());
            }
        }
        return total;
    }

    @Override
    public BigDecimal getTotalByTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return getTotalForDateRange(taxYear.startDate(), taxYear.endDate());
    }

    @Override
    public BigDecimal getTotalForDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        try (PreparedStatement pstmt = dataStore.connection()
                 .prepareStatement(SQL.get("selectExpenseAmountsByBusinessAndDateRange"))) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            return sumAmounts(pstmt);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to calculate total expenses", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getAllowableTotalByTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return getAllowableTotalForDateRange(taxYear.startDate(), taxYear.endDate());
    }

    @Override
    public BigDecimal getAllowableTotalForDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        // Every expense in the range, with the claimable part of each decided by the expense itself.
        // Filtering allowable categories in SQL as well would put the same rule in two places, and
        // the one in the database cannot see the business-use share.
        try (PreparedStatement pstmt = dataStore.connection()
                 .prepareStatement(SQL.get("findExpensesByBusinessAndDateRange"))) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            return sumBusinessUseShares(pstmt);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to calculate allowable expenses", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Map<ExpenseCategory, BigDecimal> getTotalsByCategoryForTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return findByTaxYear(taxYear).stream()
            .collect(Collectors.groupingBy(
                Expense::category,
                // The claimable share: these totals feed the Tax Summary and the SA103 breakdown.
                Collectors.reducing(BigDecimal.ZERO, Expense::allowableAmount, BigDecimal::add)
            ));
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Expense ID cannot be null");
        }
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("deleteExpenseById"))) {
            pstmt.setString(1, id.toString());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to delete expense: " + id, e);
            return false;
        }
    }

    @Override
    public long count() {
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("countExpensesByBusiness"))) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to count expenses", e);
        }
        return 0;
    }

    @Override
    public UUID getBusinessId() {
        return businessId;
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
            null, // bankTransactionId - not stored in SQLite yet
            rs.getInt("business_use_pct")
        );
    }
}
