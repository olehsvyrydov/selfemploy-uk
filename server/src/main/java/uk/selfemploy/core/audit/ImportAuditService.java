package uk.selfemploy.core.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.ImportAuditType;
import uk.selfemploy.persistence.repository.ImportAuditRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing import audit records.
 *
 * <p>Provides audit trail functionality for import operations.
 * Audit records are immutable once created (COND-F9 requirement).
 * Only status-related fields can be updated via the undo operation.</p>
 *
 * <p>Finance Conditions:</p>
 * <ul>
 *   <li>COND-F9: Audit trail must be immutable (no delete capability)</li>
 *   <li>COND-F10: Include timestamp in UTC with timezone offset</li>
 * </ul>
 */
@ApplicationScoped
public class ImportAuditService {

    private final ImportAuditRepository auditRepository;
    private final Clock clock;

    @Inject
    public ImportAuditService(ImportAuditRepository auditRepository, Clock clock) {
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    /**
     * Creates an audit record for an import operation.
     *
     * <p>This should be called after import completes successfully.
     * The audit record is immutable once created.</p>
     *
     * @param businessId the business ID
     * @param fileName the original file name
     * @param fileHash SHA-256 hash of the file content
     * @param importType the type of import
     * @param totalRecords total records in the file
     * @param importedCount number of records imported
     * @param skippedCount number of records skipped
     * @param recordIds list of UUIDs for imported records
     * @return the created audit record
     */
    public ImportAudit createAuditRecord(
            UUID businessId,
            String fileName,
            String fileHash,
            ImportAuditType importType,
            int totalRecords,
            int importedCount,
            int skippedCount,
            List<UUID> recordIds) {

        ImportAudit audit = ImportAudit.create(
            businessId,
            clock.instant(),
            fileName,
            fileHash,
            importType,
            totalRecords,
            importedCount,
            skippedCount,
            recordIds
        );

        return auditRepository.save(audit);
    }

    /**
     * Retrieves an audit record by ID.
     *
     * @param auditId the audit record ID
     * @return the audit record if found
     */
    public Optional<ImportAudit> getAuditById(UUID auditId) {
        return auditRepository.findByIdAsDomain(auditId);
    }

    /**
     * Retrieves all audit records for a business.
     *
     * @param businessId the business ID
     * @return list of audit records, ordered by import timestamp descending
     */
    public List<ImportAudit> getAuditHistory(UUID businessId) {
        return auditRepository.findByBusinessId(businessId);
    }

    /**
     * Retrieves audit records for a business within a date range.
     *
     * @param businessId the business ID
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return list of audit records within the date range
     */
    public List<ImportAudit> getAuditHistory(UUID businessId, LocalDate from, LocalDate to) {
        return auditRepository.findByBusinessIdAndDateRange(businessId, from, to);
    }

    /**
     * Retrieves all active (non-undone) audit records for a business.
     *
     * @param businessId the business ID
     * @return list of active audit records
     */
    public List<ImportAudit> getActiveAuditHistory(UUID businessId) {
        return auditRepository.findActiveByBusinessId(businessId);
    }

    /**
     * Checks if a file with the given hash has already been imported.
     *
     * @param businessId the business ID
     * @param fileHash the file hash to check
     * @return true if the file was already imported
     */
    public boolean isFileAlreadyImported(UUID businessId, String fileHash) {
        return auditRepository.existsByFileHash(businessId, fileHash);
    }

    // Note: No delete or update methods exposed (COND-F9 requirement)
    // Status updates are handled by ImportUndoService only
}
