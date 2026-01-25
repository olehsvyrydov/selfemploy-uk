package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.persistence.entity.ImportAuditEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for ImportAudit entities.
 *
 * <p>Provides data access for import audit records. These records are
 * immutable except for status changes (COND-F9 requirement).</p>
 */
@ApplicationScoped
public class ImportAuditRepository implements PanacheRepositoryBase<ImportAuditEntity, UUID> {

    /**
     * Saves an import audit record to the database.
     *
     * <p>Audit records are immutable once created. This method is only
     * called when creating a new audit record.</p>
     */
    public ImportAudit save(ImportAudit audit) {
        ImportAuditEntity entity = ImportAuditEntity.fromDomain(audit);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds an import audit by ID.
     */
    public Optional<ImportAudit> findByIdAsDomain(UUID id) {
        return findByIdOptional(id)
            .map(ImportAuditEntity::toDomain);
    }

    /**
     * Finds all import audits for a business.
     */
    public List<ImportAudit> findByBusinessId(UUID businessId) {
        return find("businessId", businessId)
            .stream()
            .map(ImportAuditEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all import audits for a business within a date range.
     *
     * @param businessId the business ID
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     */
    public List<ImportAudit> findByBusinessIdAndDateRange(UUID businessId, LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        return find("businessId = ?1 and importTimestamp >= ?2 and importTimestamp < ?3 order by importTimestamp desc",
                businessId, fromInstant, toInstant)
            .stream()
            .map(ImportAuditEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all active import audits for a business (not undone).
     */
    public List<ImportAudit> findActiveByBusinessId(UUID businessId) {
        return find("businessId = ?1 and status = ?2 order by importTimestamp desc",
                businessId, ImportAuditStatus.ACTIVE)
            .stream()
            .map(ImportAuditEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all import audits for a business by status.
     */
    public List<ImportAudit> findByBusinessIdAndStatus(UUID businessId, ImportAuditStatus status) {
        return find("businessId = ?1 and status = ?2 order by importTimestamp desc",
                businessId, status)
            .stream()
            .map(ImportAuditEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds import audits that can be undone (within undo window).
     *
     * @param businessId the business ID
     * @param cutoffTime only imports after this time can be undone
     */
    public List<ImportAudit> findUndoableImports(UUID businessId, Instant cutoffTime) {
        return find("businessId = ?1 and status = ?2 and importTimestamp > ?3 order by importTimestamp desc",
                businessId, ImportAuditStatus.ACTIVE, cutoffTime)
            .stream()
            .map(ImportAuditEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Updates the status of an import audit (for undo operation).
     *
     * <p>This is the ONLY update operation allowed on audit records
     * (COND-F9 requirement). It only changes status-related fields.</p>
     */
    public ImportAudit updateStatus(ImportAudit audit) {
        ImportAuditEntity entity = findById(audit.id());
        if (entity == null) {
            throw new IllegalArgumentException("Import audit not found: " + audit.id());
        }

        // Only update status-related fields (immutability requirement)
        entity.setStatus(audit.status());
        entity.setUndoneAt(audit.undoneAt());
        entity.setUndoneBy(audit.undoneBy());

        persist(entity);
        return entity.toDomain();
    }

    /**
     * Checks if a file with the given hash has already been imported for a business.
     */
    public boolean existsByFileHash(UUID businessId, String fileHash) {
        return count("businessId = ?1 and fileHash = ?2 and status = ?3",
                businessId, fileHash, ImportAuditStatus.ACTIVE) > 0;
    }

    /**
     * Finds the most recent import for a business.
     */
    public Optional<ImportAudit> findMostRecentByBusinessId(UUID businessId) {
        return find("businessId = ?1 order by importTimestamp desc", businessId)
            .firstResultOptional()
            .map(ImportAuditEntity::toDomain);
    }

    /**
     * Counts the total imports for a business.
     */
    public long countByBusinessId(UUID businessId) {
        return count("businessId", businessId);
    }

    /**
     * Counts active imports for a business.
     */
    public long countActiveByBusinessId(UUID businessId) {
        return count("businessId = ?1 and status = ?2", businessId, ImportAuditStatus.ACTIVE);
    }
}
