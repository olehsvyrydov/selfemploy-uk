package uk.selfemploy.common.domain;

import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.common.enums.ImportAuditType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an audit record for an import operation.
 *
 * <p>Tracks metadata about imports for auditing and undo capability:
 * <ul>
 *   <li>File information (name, hash for verification)</li>
 *   <li>Record counts (total, imported, skipped)</li>
 *   <li>List of imported record IDs for undo</li>
 *   <li>Status tracking (ACTIVE or UNDONE)</li>
 *   <li>Original file location and encryption status</li>
 *   <li>Data retention enforcement</li>
 *   <li>User identity for audit trail</li>
 * </ul>
 *
 * <p>Audit records are immutable once created. Only status-related fields
 * can be updated via the undo operation.</p>
 */
public record ImportAudit(
    UUID id,
    UUID businessId,
    Instant importTimestamp,
    String fileName,
    String fileHash,
    ImportAuditType importType,
    int totalRecords,
    int importedCount,
    int skippedCount,
    List<UUID> recordIds,
    ImportAuditStatus status,
    Instant undoneAt,
    String undoneBy,
    String originalFilePath,
    Boolean originalFileEncrypted,
    LocalDate retentionUntil,
    String importedBy
) {
    /**
     * Compact constructor for validation.
     */
    public ImportAudit {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null");
        }
        if (importTimestamp == null) {
            throw new IllegalArgumentException("importTimestamp cannot be null");
        }
        if (importType == null) {
            throw new IllegalArgumentException("importType cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (totalRecords < 0) {
            throw new IllegalArgumentException("totalRecords cannot be negative");
        }
        if (importedCount < 0) {
            throw new IllegalArgumentException("importedCount cannot be negative");
        }
        if (skippedCount < 0) {
            throw new IllegalArgumentException("skippedCount cannot be negative");
        }
        // Ensure recordIds is immutable
        recordIds = recordIds != null ? List.copyOf(recordIds) : List.of();
    }

    /**
     * Creates a new ImportAudit record for an active import (basic version).
     *
     * <p>Use this for simple imports without file storage or retention tracking.</p>
     */
    public static ImportAudit create(
            UUID businessId,
            Instant importTimestamp,
            String fileName,
            String fileHash,
            ImportAuditType importType,
            int totalRecords,
            int importedCount,
            int skippedCount,
            List<UUID> recordIds) {
        return new ImportAudit(
            UUID.randomUUID(),
            businessId,
            importTimestamp,
            fileName,
            fileHash,
            importType,
            totalRecords,
            importedCount,
            skippedCount,
            recordIds != null ? new ArrayList<>(recordIds) : List.of(),
            ImportAuditStatus.ACTIVE,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Creates a new ImportAudit record with full audit trail fields.
     *
     * <p>Use this for bank CSV imports where original file preservation,
     * encryption status, retention dates, and user identity are tracked.</p>
     */
    public static ImportAudit createWithAuditTrail(
            UUID businessId,
            Instant importTimestamp,
            String fileName,
            String fileHash,
            ImportAuditType importType,
            int totalRecords,
            int importedCount,
            int skippedCount,
            List<UUID> recordIds,
            String originalFilePath,
            boolean originalFileEncrypted,
            LocalDate retentionUntil,
            String importedBy) {
        return new ImportAudit(
            UUID.randomUUID(),
            businessId,
            importTimestamp,
            fileName,
            fileHash,
            importType,
            totalRecords,
            importedCount,
            skippedCount,
            recordIds != null ? new ArrayList<>(recordIds) : List.of(),
            ImportAuditStatus.ACTIVE,
            null,
            null,
            originalFilePath,
            originalFileEncrypted,
            retentionUntil,
            importedBy
        );
    }

    /**
     * Creates a copy marked as undone.
     */
    public ImportAudit withUndone(Instant undoneAt, String undoneBy) {
        return new ImportAudit(
            this.id,
            this.businessId,
            this.importTimestamp,
            this.fileName,
            this.fileHash,
            this.importType,
            this.totalRecords,
            this.importedCount,
            this.skippedCount,
            this.recordIds,
            ImportAuditStatus.UNDONE,
            undoneAt,
            undoneBy,
            this.originalFilePath,
            this.originalFileEncrypted,
            this.retentionUntil,
            this.importedBy
        );
    }

    /**
     * Returns true if the original file is stored and encrypted.
     */
    public boolean hasEncryptedFile() {
        return originalFilePath != null && Boolean.TRUE.equals(originalFileEncrypted);
    }

    /**
     * Returns true if this audit record has retention tracking.
     */
    public boolean hasRetentionPolicy() {
        return retentionUntil != null;
    }

    /**
     * Checks if this import can be undone.
     *
     * @return true if the import is in ACTIVE status
     */
    public boolean canUndo() {
        return status.canUndo();
    }

    /**
     * Returns the number of records that can be undone.
     */
    public int undoableRecordCount() {
        return recordIds != null ? recordIds.size() : 0;
    }
}
