package uk.selfemploy.ui.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed submission repository.
 * All operations directly query the database - no in-memory caching.
 * This ensures submission history is never lost and is always consistent.
 *
 * <p>Implements BUG-10H-001: Submission History persistence per ADR-10H-001.</p>
 *
 * <p>Follows the same pattern as {@link SqliteIncomeRepository} and
 * {@link SqliteExpenseRepository} for consistency.</p>
 */
public class SqliteSubmissionRepository {

    private final SqliteDataStore dataStore;
    private final UUID businessId;

    /**
     * Creates a new repository for the given business.
     *
     * @param businessId The business ID for all operations
     * @throws IllegalArgumentException if businessId is null
     */
    public SqliteSubmissionRepository(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.dataStore = SqliteDataStore.getInstance();
        // Ensure business exists for FK constraints
        dataStore.ensureBusinessExists(businessId);
    }

    /**
     * Saves a submission record to the database.
     *
     * @param submission The submission to save
     * @return The saved submission
     * @throws IllegalArgumentException if submission is null
     */
    public SubmissionRecord save(SubmissionRecord submission) {
        if (submission == null) {
            throw new IllegalArgumentException("Submission cannot be null");
        }
        dataStore.saveSubmission(submission);
        return submission;
    }

    /**
     * Finds all submissions for this business.
     *
     * @return All submissions sorted by submitted_at descending (newest first)
     */
    public List<SubmissionRecord> findAll() {
        return dataStore.findSubmissionsByBusinessId(businessId);
    }

    /**
     * Finds a submission by ID.
     *
     * @param id The submission ID
     * @return The submission if found
     * @throws IllegalArgumentException if id is null
     */
    public Optional<SubmissionRecord> findById(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        return dataStore.findSubmissionById(id);
    }

    /**
     * Finds submissions by tax year.
     *
     * @param taxYearStart The tax year start (e.g., 2025 for 2025/26)
     * @return Submissions for the tax year
     */
    public List<SubmissionRecord> findByTaxYear(int taxYearStart) {
        return dataStore.findSubmissionsByTaxYear(businessId, taxYearStart);
    }

    /**
     * Deletes a submission by ID.
     *
     * @param id The submission ID
     * @return true if deleted, false if not found
     * @throws IllegalArgumentException if id is null
     */
    public boolean delete(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        return dataStore.deleteSubmission(id);
    }

    /**
     * Returns the count of all submissions for this business.
     *
     * @return The submission count
     */
    public long count() {
        return dataStore.countSubmissions(businessId);
    }

    /**
     * Returns the business ID for this repository.
     *
     * @return The business ID
     */
    public UUID getBusinessId() {
        return businessId;
    }
}
