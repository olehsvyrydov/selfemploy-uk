package uk.selfemploy.ui.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and querying submission history for a single business.
 *
 * <p>The shipping desktop app implements this with {@link SqliteSubmissionRepository} (a JDBC
 * adapter). Instances are scoped to one business, fixed at construction.</p>
 */
public interface SubmissionRepository {

    /**
     * Saves (inserts or replaces) a submission record.
     *
     * @return the saved submission
     * @throws IllegalArgumentException if submission is null
     */
    SubmissionRecord save(SubmissionRecord submission);

    /**
     * Returns all submissions for this business, newest first.
     */
    List<SubmissionRecord> findAll();

    /**
     * Finds a submission by its id.
     *
     * @throws IllegalArgumentException if id is null
     */
    Optional<SubmissionRecord> findById(String id);

    /**
     * Returns this business's submissions for the given tax year (start year), newest first.
     */
    List<SubmissionRecord> findByTaxYear(int taxYearStart);

    /**
     * Deletes a submission by id.
     *
     * @return true if a row was deleted
     * @throws IllegalArgumentException if id is null
     */
    boolean delete(String id);

    /**
     * Returns the count of submissions for this business.
     */
    long count();

    /**
     * Returns the business id this repository is scoped to.
     */
    UUID getBusinessId();
}
