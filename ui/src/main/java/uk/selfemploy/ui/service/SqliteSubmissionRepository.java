package uk.selfemploy.ui.service;

import uk.selfemploy.ui.service.sql.NamedSql;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite JDBC adapter for {@link SubmissionRepository}.
 *
 * <p>Owns its own SQL (loaded from {@code /sql/submissions.sql}) and runs it against the shared
 * connection from {@link SqliteDataStore}. The submissions table and its honesty migration are
 * handled by {@code SqliteDataStore}'s schema initialisation.</p>
 *
 * <p>Follows the same pattern as {@link SqliteIncomeRepository} and {@link SqliteExpenseRepository}.</p>
 */
public class SqliteSubmissionRepository implements SubmissionRepository {

    private static final Logger LOG = Logger.getLogger(SqliteSubmissionRepository.class.getName());

    private static final NamedSql SQL = NamedSql.load("/sql/submissions.sql");

    private final SqliteDataStore dataStore;
    private final UUID businessId;

    /**
     * Creates a new repository for the given business.
     *
     * @param businessId The business ID for all operations
     * @throws IllegalArgumentException if businessId is null
     */
    public SqliteSubmissionRepository(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.dataStore = SqliteDataStore.getInstance();
        // Ensure business exists for FK constraints
        dataStore.ensureBusinessExists(businessId);
    }

    @Override
    public SubmissionRecord save(SubmissionRecord submission) {
        if (submission == null) {
            throw new IllegalArgumentException("Submission cannot be null");
        }
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("insertSubmission"))) {
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
        return submission;
    }

    @Override
    public List<SubmissionRecord> findAll() {
        List<SubmissionRecord> submissions = new ArrayList<>();
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findSubmissionsByBusiness"))) {
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

    @Override
    public Optional<SubmissionRecord> findById(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findSubmissionById"))) {
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

    @Override
    public List<SubmissionRecord> findByTaxYear(int taxYearStart) {
        List<SubmissionRecord> submissions = new ArrayList<>();
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findSubmissionsByBusinessAndTaxYear"))) {
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

    @Override
    public boolean delete(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("deleteSubmissionById"))) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to delete submission: " + id, e);
            return false;
        }
    }

    @Override
    public long count() {
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("countSubmissionsByBusiness"))) {
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

    @Override
    public UUID getBusinessId() {
        return businessId;
    }

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
}
