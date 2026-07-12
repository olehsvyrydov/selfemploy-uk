package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;
import uk.selfemploy.ui.service.sql.NamedSql;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite JDBC adapter for {@link BankTransactionRepository}.
 *
 * <p>Owns its own SQL (loaded from {@code /sql/bank-transaction.sql}) and its row mapper, running
 * against the shared connection from {@link SqliteDataStore}. The bank_transactions and
 * transaction_modification_log table DDL is handled by {@code SqliteDataStore}'s schema
 * initialisation.</p>
 */
public class SqliteBankTransactionRepository implements BankTransactionRepository {

    private static final Logger LOG = Logger.getLogger(SqliteBankTransactionRepository.class.getName());

    private static final NamedSql SQL = NamedSql.load("/sql/bank-transaction.sql");

    private final SqliteDataStore dataStore;
    private final UUID businessId;

    public SqliteBankTransactionRepository(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.dataStore = SqliteDataStore.getInstance();
    }

    @Override
    public void save(BankTransaction tx) {
        if (tx == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("insertBankTransaction"))) {
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

    @Override
    public Optional<BankTransaction> findById(UUID id) {
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("findBankTransactionById"))) {
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

    @Override
    public List<BankTransaction> findAll() {
        List<BankTransaction> transactions = new ArrayList<>();
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("findBankTransactionsByBusiness"))) {
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

    @Override
    public long countByStatus(String status) {
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("countBankTransactionsByBusinessAndStatus"))) {
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

    @Override
    public long count() {
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("countBankTransactionsByBusiness"))) {
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

    @Override
    public boolean existsByHash(String hash) {
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("existsByBusinessAndTransactionHash"))) {
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

    @Override
    public boolean softDelete(UUID id) {
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("softDeleteBankTransaction"))) {
            pstmt.setString(1, Instant.now().toString());
            pstmt.setString(2, "local-user");
            pstmt.setString(3, "User-initiated deletion");
            pstmt.setString(4, id.toString());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to soft-delete bank transaction: " + id, e);
            return false;
        }
    }

    @Override
    public void logModification(UUID bankTransactionId, String modificationType, String fieldName,
                                String previousValue, String newValue, String modifiedBy) {
        try (PreparedStatement pstmt = dataStore.connection().prepareStatement(SQL.get("insertModificationLog"))) {
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

    @Override
    public List<Map<String, String>> findModificationLogs(UUID bankTransactionId) {
        List<Map<String, String>> logs = new ArrayList<>();
        try (PreparedStatement pstmt =
                 dataStore.connection().prepareStatement(SQL.get("findModificationLogsByBankTransaction"))) {
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

    @Override
    public UUID getBusinessId() {
        return businessId;
    }

    private BankTransaction mapBankTransaction(ResultSet rs) throws SQLException {
        String incomeIdStr = rs.getString("income_id");
        String expenseIdStr = rs.getString("expense_id");
        String confidenceStr = rs.getString("confidence_score");
        String categoryStr = rs.getString("suggested_category");
        String updatedAtStr = rs.getString("updated_at");
        String deletedAtStr = rs.getString("deleted_at");

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
}
