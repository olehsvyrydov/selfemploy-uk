package uk.selfemploy.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import uk.selfemploy.common.domain.AnnualSubmissionSaga;
import uk.selfemploy.persistence.entity.AnnualSubmissionSagaEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Annual Submission Saga persistence.
 *
 * <p>Enables saga state persistence and retrieval for resume capability.
 */
@ApplicationScoped
public class AnnualSubmissionSagaRepository {

    private final EntityManager entityManager;

    @Inject
    public AnnualSubmissionSagaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Saves or updates a saga.
     *
     * @param saga The saga to save
     * @return The saved saga
     */
    @Transactional
    public AnnualSubmissionSaga save(AnnualSubmissionSaga saga) {
        AnnualSubmissionSagaEntity entity = AnnualSubmissionSagaEntity.fromDomain(saga);
        entityManager.merge(entity);
        return entity.toDomain();
    }

    /**
     * Finds a saga by ID.
     *
     * @param id The saga ID
     * @return Optional containing the saga if found
     */
    public Optional<AnnualSubmissionSaga> findById(UUID id) {
        AnnualSubmissionSagaEntity entity = entityManager.find(AnnualSubmissionSagaEntity.class, id);
        return Optional.ofNullable(entity).map(AnnualSubmissionSagaEntity::toDomain);
    }

    /**
     * Finds a saga by NINO and tax year.
     *
     * <p>Used to check if a submission already exists for a tax year.
     *
     * @param nino National Insurance Number
     * @param taxYearStart Tax year start year
     * @return Optional containing the saga if found
     */
    public Optional<AnnualSubmissionSaga> findByNinoAndTaxYear(String nino, int taxYearStart) {
        try {
            AnnualSubmissionSagaEntity entity = entityManager.createQuery(
                "SELECT s FROM AnnualSubmissionSagaEntity s WHERE s.nino = :nino AND s.taxYearStart = :taxYearStart",
                AnnualSubmissionSagaEntity.class
            )
            .setParameter("nino", nino)
            .setParameter("taxYearStart", taxYearStart)
            .getSingleResult();

            return Optional.of(entity.toDomain());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Deletes a saga by ID.
     *
     * @param id The saga ID
     */
    @Transactional
    public void deleteById(UUID id) {
        AnnualSubmissionSagaEntity entity = entityManager.find(AnnualSubmissionSagaEntity.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }
}
