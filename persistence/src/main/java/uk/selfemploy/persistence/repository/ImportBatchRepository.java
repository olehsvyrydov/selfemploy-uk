package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.ImportBatch;
import uk.selfemploy.persistence.entity.ImportBatchEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for ImportBatch entities.
 */
@ApplicationScoped
public class ImportBatchRepository implements PanacheRepositoryBase<ImportBatchEntity, UUID> {

    /**
     * Saves an import batch to the database.
     *
     * @param batch the import batch to save
     * @return the saved import batch
     */
    public ImportBatch save(ImportBatch batch) {
        ImportBatchEntity entity = ImportBatchEntity.fromDomain(batch);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds an import batch by ID.
     *
     * @param id the import batch ID
     * @return Optional containing the import batch if found
     */
    public Optional<ImportBatch> findByIdAsDomain(UUID id) {
        return findByIdOptional(id)
            .map(ImportBatchEntity::toDomain);
    }

    /**
     * Finds all import batches for a business.
     *
     * @param businessId the business ID
     * @return list of import batches for the business
     */
    public List<ImportBatch> findByBusinessId(UUID businessId) {
        return find("businessId", businessId)
            .stream()
            .map(ImportBatchEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all import batches for a business, ordered by import date descending.
     *
     * @param businessId the business ID
     * @return list of import batches for the business, most recent first
     */
    public List<ImportBatch> findByBusinessIdOrderByImportedAtDesc(UUID businessId) {
        return find("businessId = ?1 ORDER BY importedAt DESC", businessId)
            .stream()
            .map(ImportBatchEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Deletes an import batch by ID.
     *
     * @param id the import batch ID
     * @return true if deleted, false if not found
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return deleteById(id);
    }
}
