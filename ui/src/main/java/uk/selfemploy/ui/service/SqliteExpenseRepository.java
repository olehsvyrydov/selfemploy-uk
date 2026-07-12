package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.service.sql.NamedSql;

import java.math.BigDecimal;
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
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.dataStore = SqliteDataStore.getInstance();
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
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("sumExpensesByBusinessAndDateRange"))) {
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
        List<String> allowableCategories = Arrays.stream(ExpenseCategory.values())
            .filter(ExpenseCategory::isAllowable)
            .map(Enum::name)
            .toList();
        if (allowableCategories.isEmpty()) {
            return BigDecimal.ZERO;
        }

        String placeholders = allowableCategories.stream().map(c -> "?").collect(Collectors.joining(","));
        String sql = String.format(SQL.get("sumAllowableExpensesByBusinessAndDateRange"), placeholders);
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(sql)) {
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

    @Override
    public Map<ExpenseCategory, BigDecimal> getTotalsByCategoryForTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return findByTaxYear(taxYear).stream()
            .collect(Collectors.groupingBy(
                Expense::category,
                Collectors.reducing(BigDecimal.ZERO, Expense::amount, BigDecimal::add)
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
            null  // bankTransactionId - not stored in SQLite yet
        );
    }
}
