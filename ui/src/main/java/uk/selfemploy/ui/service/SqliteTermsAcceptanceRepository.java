package uk.selfemploy.ui.service;

import uk.selfemploy.persistence.repository.TermsAcceptanceRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * SQLite-backed implementation of TermsAcceptanceRepository for the UI layer.
 *
 * <p>Since the UI doesn't use Quarkus CDI, this provides a standalone implementation
 * that delegates to SqliteDataStore for persistence.</p>
 *
 * <p>SE-508: Terms of Service UI</p>
 */
public class SqliteTermsAcceptanceRepository {

    private static final Logger LOG = Logger.getLogger(SqliteTermsAcceptanceRepository.class.getName());

    private final SqliteDataStore dataStore;

    public SqliteTermsAcceptanceRepository() {
        this.dataStore = SqliteDataStore.getInstance();
    }

    /**
     * Constructor for testing with custom data store.
     */
    SqliteTermsAcceptanceRepository(SqliteDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Saves a new terms acceptance.
     *
     * @param tosVersion          The version of the ToS
     * @param acceptedAt          The timestamp of acceptance
     * @param scrollCompletedAt   The timestamp when user scrolled to bottom
     * @param applicationVersion  The application version
     * @return true if saved successfully, false otherwise
     */
    public boolean save(String tosVersion, Instant acceptedAt, Instant scrollCompletedAt, String applicationVersion) {
        return dataStore.saveTermsAcceptance(tosVersion, acceptedAt, scrollCompletedAt, applicationVersion);
    }

    /**
     * Gets the most recently accepted ToS version.
     *
     * @return Optional containing the version string, or empty if no acceptances exist
     */
    public Optional<String> getLatestAcceptedVersion() {
        return dataStore.getLatestAcceptedTermsVersion();
    }

    /**
     * Gets the timestamp of the most recent acceptance.
     *
     * @return Optional containing the timestamp, or empty if no acceptances exist
     */
    public Optional<Instant> getLatestAcceptanceTimestamp() {
        return dataStore.getLatestTermsAcceptanceTimestamp();
    }

    /**
     * Gets the scroll completed timestamp of the most recent acceptance.
     *
     * @return Optional containing the timestamp, or empty if no acceptances exist
     */
    public Optional<Instant> getLatestScrollCompletedTimestamp() {
        return dataStore.getLatestTermsScrollCompletedTimestamp();
    }
}
