package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.persistence.entity.SubmissionEntity;
import uk.selfemploy.persistence.exception.DuplicateSubmissionException;

import java.time.LocalDate;
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
     *
     * @param submission the submission to save
     * @return the saved submission
     * @throws DuplicateSubmissionException if a submission already exists for the same
     *         (business_id, tax_year_start, type) combination
     */
    public Submission save(Submission submission) {
        SubmissionEntity entity = SubmissionEntity.fromDomain(submission);
        try {
            persist(entity);
            getEntityManager().flush(); // Force constraint check
            return entity.toDomain();
        } catch (PersistenceException e) {
            if (isDuplicateConstraintViolation(e)) {
                throw new DuplicateSubmissionException(
                    submission.businessId(),
                    submission.taxYear().startYear(),
                    submission.type(),
                    e
                );
            }
            throw e;
        }
    }

    /**
     * Saves a submission if no duplicate exists, otherwise returns the existing submission.
     *
     * @param submission the submission to save
     * @return the saved submission or the existing duplicate
     */
    public Submission saveIfNotExists(Submission submission) {
        // Check for existing submission first
        Optional<Submission> existing = findExistingSubmission(
            submission.businessId(),
            submission.taxYear(),
            submission.type()
        );
        if (existing.isPresent()) {
            return existing.get();
        }
        return save(submission);
    }

    /**
     * Finds an existing submission by business ID, tax year, and type.
     */
    public Optional<Submission> findExistingSubmission(UUID businessId, TaxYear taxYear, SubmissionType type) {
        return find("businessId = ?1 and taxYearStart = ?2 and type = ?3",
                businessId, taxYear.startYear(), type)
            .firstResultOptional()
            .map(SubmissionEntity::toDomain);
    }

    /**
     * Checks if a persistence exception is due to a duplicate constraint violation.
     */
    private boolean isDuplicateConstraintViolation(PersistenceException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                // H2 uses uppercase and includes schema prefix
                return constraintName != null &&
                       constraintName.toUpperCase().contains("UK_SUBMISSION_BUSINESS_YEAR_TYPE");
            }
            // Also check for H2 specific SQL exception with unique constraint message
            if (cause instanceof java.sql.SQLException sqlException) {
                String message = sqlException.getMessage();
                if (message != null && message.contains("UK_SUBMISSION_BUSINESS_YEAR_TYPE")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
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

    /**
     * Finds all submissions for a specific UTR.
     *
     * @param utr the Unique Taxpayer Reference (10 digits)
     * @return list of submissions for the given UTR
     */
    public List<Submission> findByUtr(String utr) {
        return find("utr", utr)
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all submissions for a specific NINO.
     *
     * @param nino the National Insurance Number
     * @return list of submissions for the given NINO
     */
    public List<Submission> findByNino(String nino) {
        return find("nino", nino)
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all submissions for a specific UTR and tax year.
     *
     * @param utr     the Unique Taxpayer Reference
     * @param taxYear the tax year
     * @return list of submissions
     */
    public List<Submission> findByUtrAndTaxYear(String utr, TaxYear taxYear) {
        return find("utr = ?1 and taxYearStart = ?2", utr, taxYear.startYear())
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all submissions for a specific NINO and tax year.
     *
     * @param nino    the National Insurance Number
     * @param taxYear the tax year
     * @return list of submissions
     */
    public List<Submission> findByNinoAndTaxYear(String nino, TaxYear taxYear) {
        return find("nino = ?1 and taxYearStart = ?2", nino, taxYear.startYear())
            .stream()
            .map(SubmissionEntity::toDomain)
            .collect(Collectors.toList());
    }

    // === Retention Policy Methods (SE-SH-002) ===

    /**
     * Finds all submissions where retention period has expired.
     * These are candidates for deletion (after approval).
     *
     * @param currentDate the date to compare retention against
     * @return list of submissions with expired retention
     */
    public List<SubmissionEntity> findByRetentionExpired(LocalDate currentDate) {
        return find("retentionRequiredUntil < ?1", currentDate).list();
    }

    /**
     * Finds all submissions that are marked as deletable.
     * These have expired retention AND deletion has been approved.
     *
     * @return list of deletable submissions
     */
    public List<SubmissionEntity> findDeletable() {
        return find("isDeletable = true").list();
    }

    /**
     * Finds all submissions for a business where retention has expired.
     *
     * @param businessId  the business ID
     * @param currentDate the date to compare retention against
     * @return list of submissions with expired retention
     */
    public List<SubmissionEntity> findByBusinessIdAndRetentionExpired(UUID businessId, LocalDate currentDate) {
        return find("businessId = ?1 and retentionRequiredUntil < ?2", businessId, currentDate).list();
    }

    /**
     * Approves deletion for a submission.
     * Sets is_deletable to true and records approval details.
     *
     * @param submissionId the submission ID
     * @param approvedBy   who approved the deletion
     * @param reason       reason for deletion
     */
    public void approveDeletion(UUID submissionId, String approvedBy, String reason) {
        SubmissionEntity entity = findById(submissionId);
        if (entity != null) {
            entity.setIsDeletable(true);
            entity.setDeletionApprovedAt(java.time.Instant.now());
            entity.setDeletionApprovedBy(approvedBy);
            entity.setDeletionReason(reason);
            persist(entity);
        }
    }

    /**
     * Counts submissions with expired retention.
     *
     * @param currentDate the date to compare retention against
     * @return count of submissions with expired retention
     */
    public long countByRetentionExpired(LocalDate currentDate) {
        return count("retentionRequiredUntil < ?1", currentDate);
    }

    /**
     * Counts deletable submissions.
     *
     * @return count of deletable submissions
     */
    public long countDeletable() {
        return count("isDeletable = true");
    }

    /**
     * Updates retention date for a submission.
     *
     * @param submissionId        the submission ID
     * @param retentionRequiredUntil the new retention date
     */
    public void updateRetentionDate(UUID submissionId, LocalDate retentionRequiredUntil) {
        SubmissionEntity entity = findById(submissionId);
        if (entity != null) {
            entity.setRetentionRequiredUntil(retentionRequiredUntil);
            persist(entity);
        }
    }

    // === Saga Sync Methods (SE-SH-003) ===

    /**
     * Finds a submission by saga ID.
     * Used for idempotency checks when syncing saga completion.
     *
     * @param sagaId the annual submission saga ID
     * @return the submission if found, or empty
     */
    public java.util.Optional<SubmissionEntity> findBySagaId(UUID sagaId) {
        return find("sagaId", sagaId).firstResultOptional();
    }

    /**
     * Checks if a submission exists for a saga.
     *
     * @param sagaId the saga ID
     * @return true if a submission exists for this saga
     */
    public boolean existsBySagaId(UUID sagaId) {
        return count("sagaId = ?1", sagaId) > 0;
    }

    /**
     * Finds submission by calculation ID.
     *
     * @param calculationId the HMRC calculation ID
     * @return the submission if found, or empty
     */
    public java.util.Optional<SubmissionEntity> findByCalculationId(String calculationId) {
        return find("calculationId", calculationId).firstResultOptional();
    }

    /**
     * Creates or updates a submission from a completed saga.
     * This method is idempotent - if a submission already exists for this saga,
     * it will be updated rather than creating a duplicate.
     *
     * @param saga       the completed annual submission saga
     * @param businessId the business ID
     * @param nino       the NINO
     * @param utr        the UTR (optional)
     * @return the created or updated submission entity
     */
    public SubmissionEntity createOrUpdateFromSaga(
            uk.selfemploy.common.domain.AnnualSubmissionSaga saga,
            UUID businessId, String nino, String utr) {

        // Check for existing submission (idempotency)
        java.util.Optional<SubmissionEntity> existing = findBySagaId(saga.id());
        if (existing.isPresent()) {
            SubmissionEntity entity = existing.get();
            entity.updateFromSaga(saga);
            entity.setUpdatedAt(java.time.Instant.now());
            persist(entity);
            return entity;
        }

        // Create new submission
        SubmissionEntity entity = new SubmissionEntity();
        entity.setId(UUID.randomUUID());
        entity.setBusinessId(businessId);
        entity.setType(uk.selfemploy.common.enums.SubmissionType.ANNUAL);
        entity.setTaxYearStart(saga.taxYear().startYear());
        entity.setPeriodStart(saga.taxYear().startDate());
        entity.setPeriodEnd(saga.taxYear().endDate());

        if (saga.calculationResult() != null) {
            entity.setTotalIncome(saga.calculationResult().totalIncome());
            entity.setTotalExpenses(saga.calculationResult().totalExpenses());
            entity.setNetProfit(saga.calculationResult().netProfit());
        } else {
            entity.setTotalIncome(java.math.BigDecimal.ZERO);
            entity.setTotalExpenses(java.math.BigDecimal.ZERO);
            entity.setNetProfit(java.math.BigDecimal.ZERO);
        }

        entity.setStatus(uk.selfemploy.common.enums.SubmissionStatus.ACCEPTED);
        entity.setHmrcReference(saga.hmrcConfirmation());
        entity.setSubmittedAt(saga.createdAt());
        entity.setUpdatedAt(java.time.Instant.now());
        entity.setNino(nino);
        entity.setUtr(utr);
        entity.updateFromSaga(saga);
        entity.calculateRetentionDate();

        persist(entity);
        return entity;
    }
}
