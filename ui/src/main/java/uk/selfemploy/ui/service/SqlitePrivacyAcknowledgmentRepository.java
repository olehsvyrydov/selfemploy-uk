package uk.selfemploy.ui.service;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * SQLite-backed implementation of PrivacyAcknowledgmentRepository for the UI layer.
 *
 * <p>Since the UI doesn't use Quarkus CDI, this provides a standalone implementation
 * that delegates to SqliteDataStore for persistence.</p>
 *
 * <p>SE-507: Privacy Notice UI</p>
 */
public class SqlitePrivacyAcknowledgmentRepository {

    private static final Logger LOG = Logger.getLogger(SqlitePrivacyAcknowledgmentRepository.class.getName());

    private final SqliteDataStore dataStore;

    public SqlitePrivacyAcknowledgmentRepository() {
        this.dataStore = SqliteDataStore.getInstance();
    }

    /**
     * Constructor for testing with custom data store.
     */
    SqlitePrivacyAcknowledgmentRepository(SqliteDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Saves a new privacy acknowledgment.
     *
     * @param privacyVersion     The version of the privacy notice
     * @param acknowledgedAt     The timestamp of acknowledgment
     * @param applicationVersion The application version
     * @return true if saved successfully, false otherwise
     */
    public boolean save(String privacyVersion, Instant acknowledgedAt, String applicationVersion) {
        return dataStore.savePrivacyAcknowledgment(privacyVersion, acknowledgedAt, applicationVersion);
    }

    /**
     * Gets the most recently acknowledged privacy notice version.
     *
     * @return Optional containing the version string, or empty if no acknowledgments exist
     */
    public Optional<String> getLatestAcknowledgedVersion() {
        return dataStore.getLatestAcknowledgedPrivacyVersion();
    }

    /**
     * Gets the timestamp of the most recent acknowledgment.
     *
     * @return Optional containing the timestamp, or empty if no acknowledgments exist
     */
    public Optional<Instant> getLatestAcknowledgmentTimestamp() {
        return dataStore.getLatestPrivacyAcknowledgmentTimestamp();
    }
}
