package uk.selfemploy.ui.service;

import java.util.Optional;

/**
 * Repository interface for persisting wizard progress.
 *
 * <p>Implementations must ensure that sensitive data (like NINO) is encrypted at rest.
 */
public interface WizardProgressRepository {

    /**
     * Finds wizard progress by wizard type.
     *
     * @param wizardType The type of wizard (e.g., "hmrc_connection")
     * @return The progress if found, or empty if no progress exists
     * @throws IllegalArgumentException if wizardType is null or blank
     */
    Optional<WizardProgress> findByType(String wizardType);

    /**
     * Saves wizard progress, inserting or updating as needed.
     *
     * <p>If progress for this wizard type already exists, it will be updated.
     * Otherwise, a new record will be created.
     *
     * @param progress The progress to save
     * @return The saved progress
     * @throws IllegalArgumentException if progress is null
     */
    WizardProgress save(WizardProgress progress);

    /**
     * Deletes wizard progress by wizard type.
     *
     * <p>This should be called when the wizard completes successfully to clear
     * the saved state.
     *
     * @param wizardType The type of wizard to delete progress for
     * @return true if a record was deleted, false if no record existed
     * @throws IllegalArgumentException if wizardType is null or blank
     */
    boolean deleteByType(String wizardType);
}
