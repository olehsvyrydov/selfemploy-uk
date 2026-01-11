package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.persistence.entity.SubmissionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for Submission entities.
 */
@ApplicationScoped
public class SubmissionRepository implements PanacheRepositoryBase<SubmissionEntity, UUID> {

    /**
     * Saves a submission to the database.
     */
    public Submission save(Submission submission) {
        SubmissionEntity entity = SubmissionEntity.fromDomain(submission);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds a submission by ID.
     */
    public Optional<Submission> findByIdAsDomain(UUID id) {
        return findByIdOptional(id)
            .map(SubmissionEntity::toDomain);
    }

    /**
     * Finds all submissions for a business.
     */
    public List<Submission> findByBusinessId(UUID businessId) {
        return find("businessId", businessId)
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all submissions for a business in a specific tax year.
     */
    public List<Submission> findByBusinessIdAndTaxYear(UUID businessId, TaxYear taxYear) {
        return find("businessId = ?1 and taxYearStart = ?2", businessId, taxYear.startYear())
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds a specific quarterly submission for a business.
     */
    public Optional<Submission> findQuarterlySubmission(UUID businessId, TaxYear taxYear, Quarter quarter) {
        SubmissionType type = switch (quarter) {
            case Q1 -> SubmissionType.QUARTERLY_Q1;
            case Q2 -> SubmissionType.QUARTERLY_Q2;
            case Q3 -> SubmissionType.QUARTERLY_Q3;
            case Q4 -> SubmissionType.QUARTERLY_Q4;
        };

        return find("businessId = ?1 and taxYearStart = ?2 and type = ?3",
                businessId, taxYear.startYear(), type)
            .firstResultOptional()
            .map(SubmissionEntity::toDomain);
    }

    /**
     * Finds the annual submission for a business in a specific tax year.
     */
    public Optional<Submission> findAnnualSubmission(UUID businessId, TaxYear taxYear) {
        return find("businessId = ?1 and taxYearStart = ?2 and type = ?3",
                businessId, taxYear.startYear(), SubmissionType.ANNUAL)
            .firstResultOptional()
            .map(SubmissionEntity::toDomain);
    }

    /**
     * Checks if a quarterly submission already exists.
     */
    public boolean existsQuarterlySubmission(UUID businessId, TaxYear taxYear, Quarter quarter) {
        return findQuarterlySubmission(businessId, taxYear, quarter).isPresent();
    }

    /**
     * Checks if an annual submission already exists.
     */
    public boolean existsAnnualSubmission(UUID businessId, TaxYear taxYear) {
        return findAnnualSubmission(businessId, taxYear).isPresent();
    }

    /**
     * Finds all successful submissions for a business.
     */
    public List<Submission> findSuccessfulSubmissions(UUID businessId) {
        return find("businessId = ?1 and (status = ?2 or status = ?3)",
                businessId, SubmissionStatus.ACCEPTED, SubmissionStatus.SUBMITTED)
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all failed submissions for a business.
     */
    public List<Submission> findFailedSubmissions(UUID businessId) {
        return find("businessId = ?1 and status = ?2", businessId, SubmissionStatus.REJECTED)
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all submissions by status.
     */
    public List<Submission> findByStatus(UUID businessId, SubmissionStatus status) {
        return find("businessId = ?1 and status = ?2", businessId, status)
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Updates a submission.
     */
    public Submission update(Submission submission) {
        SubmissionEntity entity = findById(submission.id());
        if (entity == null) {
            throw new IllegalArgumentException("Submission not found: " + submission.id());
        }
        entity.setStatus(submission.status());
        entity.setHmrcReference(submission.hmrcReference());
        entity.setErrorMessage(submission.errorMessage());
        entity.setUpdatedAt(submission.updatedAt());
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Deletes a submission by ID.
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return deleteById(id);
    }

    /**
     * Counts submissions for a business in a tax year.
     */
    public long countByTaxYear(UUID businessId, TaxYear taxYear) {
        return count("businessId = ?1 and taxYearStart = ?2", businessId, taxYear.startYear());
    }

    /**
     * Finds all quarterly submissions for a tax year, ordered by quarter.
     */
    public List<Submission> findQuarterlySubmissions(UUID businessId, TaxYear taxYear) {
        return find("businessId = ?1 and taxYearStart = ?2 and type != ?3 order by type",
                businessId, taxYear.startYear(), SubmissionType.ANNUAL)
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }
}
