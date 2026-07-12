package uk.selfemploy.ui.service;

import java.time.Instant;
import java.util.Optional;

/**
 * Port for persisting and reading privacy-notice acknowledgments.
 *
 * <p>The shipping desktop app implements this with {@link SqlitePrivacyAcknowledgmentRepository}
 * (a JDBC adapter). Callers depend on the port rather than the SQLite adapter.</p>
 */
public interface PrivacyAcknowledgmentRepository {

    /**
     * Saves a new privacy acknowledgment.
     *
     * @return true if saved successfully, false otherwise
     */
    boolean save(String privacyVersion, Instant acknowledgedAt, String applicationVersion);

    /**
     * Returns the most recently acknowledged privacy-notice version, or empty if none.
     */
    Optional<String> getLatestAcknowledgedVersion();

    /**
     * Returns the timestamp of the most recent acknowledgment, or empty if none.
     */
    Optional<Instant> getLatestAcknowledgmentTimestamp();
}
