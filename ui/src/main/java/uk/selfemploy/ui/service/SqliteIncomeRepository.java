package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.ui.service.sql.NamedSql;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * SQLite JDBC adapter for {@link IncomeRepository}.
 *
 * <p>Owns its own SQL (loaded from {@code /sql/income.sql}) and its row mapper, running against
 * the shared connection from {@link SqliteDataStore}. The income table DDL is handled by
 * {@code SqliteDataStore}'s schema initialisation.</p>
 */
public class SqliteIncomeRepository implements IncomeRepository {

    private static final Logger LOG = Logger.getLogger(SqliteIncomeRepository.class.getName());

    private static final NamedSql SQL = NamedSql.load("/sql/income.sql");

    private final SqliteDataStore dataStore;
    private final UUID businessId;

    public SqliteIncomeRepository(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.dataStore = SqliteDataStore.getInstance();
        dataStore.ensureBusinessExists(businessId);
    }

    @Override
    public Income save(Income income) {
        if (income == null) {
            throw new IllegalArgumentException("Income cannot be null");
        }
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("insertIncome"))) {
            pstmt.setString(1, income.id().toString());
            pstmt.setString(2, income.businessId().toString());
            pstmt.setString(3, income.date().toString());
            pstmt.setString(4, income.amount().toPlainString());
            pstmt.setString(5, income.description());
            pstmt.setString(6, income.category().name());
            pstmt.setString(7, income.reference());
            pstmt.setString(8, income.clientName());
            pstmt.setString(9, income.status() != null ? income.status().name() : IncomeStatus.PAID.name());
            pstmt.executeUpdate();
            LOG.fine("Saved income: " + income.id());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save income: " + income.id(), e);
            throw new DataStoreException("Failed to save income", e);
        }
        return income;
    }

    @Override
    public Optional<Income> findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Income ID cannot be null");
        }
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("findIncomeById"))) {
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

    @Override
    public List<Income> findAll() {
        List<Income> result = new ArrayList<>();
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("findIncomeByBusiness"))) {
            pstmt.setString(1, businessId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(mapIncome(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find income by business ID", e);
        }
        return result;
    }

    @Override
    public List<Income> findByTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return findByDateRange(taxYear.startDate(), taxYear.endDate());
    }

    @Override
    public List<Income> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        List<Income> result = new ArrayList<>();
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findIncomeByBusinessAndDateRange"))) {
            pstmt.setString(1, businessId.toString());
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(mapIncome(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find income by date range", e);
        }
        return result;
    }

    @Override
    public List<Income> findByCategory(IncomeCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        return findAll().stream()
            .filter(i -> i.category() == category)
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
                 dataStore.connection().prepareStatement(SQL.get("sumIncomeByBusinessAndDateRange"))) {
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

    @Override
    public boolean delete(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Income ID cannot be null");
        }
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("deleteIncomeById"))) {
            pstmt.setString(1, id.toString());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to delete income: " + id, e);
            return false;
        }
    }

    @Override
    public long count() {
        return findAll().size();
    }

    @Override
    public UUID getBusinessId() {
        return businessId;
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
            null, // bankTransactionId - not stored in SQLite yet
            rs.getString("client_name"),
            parseIncomeStatus(rs.getString("status"))
        );
    }

    private static IncomeStatus parseIncomeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return IncomeStatus.PAID;
        }
        try {
            return IncomeStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return IncomeStatus.PAID;
        }
    }
}
