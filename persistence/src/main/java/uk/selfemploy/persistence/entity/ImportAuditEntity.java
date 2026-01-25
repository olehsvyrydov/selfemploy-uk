package uk.selfemploy.persistence.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.common.enums.ImportAuditType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for ImportAudit.
 *
 * <p>Maps the import_audit table created in V10 migration.</p>
 */
@Entity
@Table(name = "import_audit")
public class ImportAuditEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "import_timestamp", nullable = false)
    private Instant importTimestamp;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", nullable = false, length = 50)
    private ImportAuditType importType;

    @Column(name = "total_records", nullable = false)
    private int totalRecords;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "record_ids", columnDefinition = "TEXT")
    private String recordIdsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ImportAuditStatus status;

    @Column(name = "undone_at")
    private Instant undoneAt;

    @Column(name = "undone_by")
    private String undoneBy;

    // Default constructor for JPA
    public ImportAuditEntity() {}

    /**
     * Creates a JPA entity from a domain ImportAudit.
     */
    public static ImportAuditEntity fromDomain(ImportAudit audit) {
        ImportAuditEntity entity = new ImportAuditEntity();
        entity.id = audit.id();
        entity.businessId = audit.businessId();
        entity.importTimestamp = audit.importTimestamp();
        entity.fileName = audit.fileName();
        entity.fileHash = audit.fileHash();
        entity.importType = audit.importType();
        entity.totalRecords = audit.totalRecords();
        entity.importedCount = audit.importedCount();
        entity.skippedCount = audit.skippedCount();
        entity.recordIdsJson = serializeUuidList(audit.recordIds());
        entity.status = audit.status();
        entity.undoneAt = audit.undoneAt();
        entity.undoneBy = audit.undoneBy();
        return entity;
    }

    /**
     * Converts this entity to a domain ImportAudit.
     */
    public ImportAudit toDomain() {
        return new ImportAudit(
            id,
            businessId,
            importTimestamp,
            fileName,
            fileHash,
            importType,
            totalRecords,
            importedCount,
            skippedCount,
            deserializeUuidList(recordIdsJson),
            status,
            undoneAt,
            undoneBy
        );
    }

    private static String serializeUuidList(List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(uuids);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize UUID list", e);
        }
    }

    private static List<UUID> deserializeUuidList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize UUID list", e);
        }
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID businessId) { this.businessId = businessId; }

    public Instant getImportTimestamp() { return importTimestamp; }
    public void setImportTimestamp(Instant importTimestamp) { this.importTimestamp = importTimestamp; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public ImportAuditType getImportType() { return importType; }
    public void setImportType(ImportAuditType importType) { this.importType = importType; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public String getRecordIdsJson() { return recordIdsJson; }
    public void setRecordIdsJson(String recordIdsJson) { this.recordIdsJson = recordIdsJson; }

    public ImportAuditStatus getStatus() { return status; }
    public void setStatus(ImportAuditStatus status) { this.status = status; }

    public Instant getUndoneAt() { return undoneAt; }
    public void setUndoneAt(Instant undoneAt) { this.undoneAt = undoneAt; }

    public String getUndoneBy() { return undoneBy; }
    public void setUndoneBy(String undoneBy) { this.undoneBy = undoneBy; }
}
