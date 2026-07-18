package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.common.enums.ImportAuditType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SQLite persistence for {@link ImportAudit} on the desktop runtime. Backs the Import History screen
 * and per-import undo: each bank import writes one audit row, whose {@code record_ids} carry the
 * staged transaction ids so an import can be reversed exactly.
 */
public class SqliteImportAuditRepository {

    private final SqliteDataStore dataStore;

    public SqliteImportAuditRepository() {
        this.dataStore = SqliteDataStore.getInstance();
    }

    /** Inserts or replaces an audit record. */
    public void save(ImportAudit audit) {
        String sql = "INSERT OR REPLACE INTO import_audit "
            + "(id, business_id, import_timestamp, file_name, file_hash, import_type, "
            + "total_records, imported_count, skipped_count, record_ids, status, undone_at, undone_by) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = dataStore.connection().prepareStatement(sql)) {
            ps.setString(1, audit.id().toString());
            ps.setString(2, audit.businessId().toString());
            ps.setString(3, audit.importTimestamp().toString());
            ps.setString(4, audit.fileName());
            ps.setString(5, audit.fileHash());
            ps.setString(6, audit.importType().name());
            ps.setInt(7, audit.totalRecords());
            ps.setInt(8, audit.importedCount());
            ps.setInt(9, audit.skippedCount());
            ps.setString(10, serializeIds(audit.recordIds()));
            ps.setString(11, audit.status().name());
            ps.setString(12, audit.undoneAt() != null ? audit.undoneAt().toString() : null);
            ps.setString(13, audit.undoneBy());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save import audit " + audit.id(), e);
        }
    }

    /** Returns a business's imports, most recent first. */
    public List<ImportAudit> findByBusinessId(UUID businessId) {
        String sql = "SELECT * FROM import_audit WHERE business_id = ? ORDER BY import_timestamp DESC";
        List<ImportAudit> results = new ArrayList<>();
        try (PreparedStatement ps = dataStore.connection().prepareStatement(sql)) {
            ps.setString(1, businessId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list import audits for business " + businessId, e);
        }
        return results;
    }

    public Optional<ImportAudit> findById(UUID id) {
        String sql = "SELECT * FROM import_audit WHERE id = ?";
        try (PreparedStatement ps = dataStore.connection().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load import audit " + id, e);
        }
    }

    /** Updates an import's status (e.g. to UNDONE), recording who undid it and when. */
    public boolean updateStatus(UUID id, ImportAuditStatus status, Instant undoneAt, String undoneBy) {
        String sql = "UPDATE import_audit SET status = ?, undone_at = ?, undone_by = ? WHERE id = ?";
        try (PreparedStatement ps = dataStore.connection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, undoneAt != null ? undoneAt.toString() : null);
            ps.setString(3, undoneBy);
            ps.setString(4, id.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update import audit status " + id, e);
        }
    }

    private static String serializeIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    private static List<UUID> deserializeIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(UUID::fromString)
            .toList();
    }

    private static ImportAudit map(ResultSet rs) throws SQLException {
        Instant undoneAt = rs.getString("undone_at") != null
            ? Instant.parse(rs.getString("undone_at")) : null;
        return new ImportAudit(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("business_id")),
            Instant.parse(rs.getString("import_timestamp")),
            rs.getString("file_name"),
            rs.getString("file_hash"),
            ImportAuditType.valueOf(rs.getString("import_type")),
            rs.getInt("total_records"),
            rs.getInt("imported_count"),
            rs.getInt("skipped_count"),
            deserializeIds(rs.getString("record_ids")),
            ImportAuditStatus.valueOf(rs.getString("status")),
            undoneAt,
            rs.getString("undone_by"),
            null, null, null, null);
    }
}
