package uk.selfemploy.core.service;

import java.time.Instant;
import java.util.Optional;

/**
 * Service contract for managing privacy notice acknowledgments.
 *
 * <p>Handles storage and retrieval of user privacy acknowledgments with version tracking.
 * Persistence is supplied by concrete subclasses (the desktop app's SQLite-backed
 * implementation), so this type carries no storage dependency.</p>
 */
public abstract class PrivacyAcknowledgmentService {

    protected PrivacyAcknowledgmentService() {
    }

    /**
     * Saves a privacy notice acknowledgment.
     *
     * @param privacyVersion      The version of the privacy notice being acknowledged
     * @param acknowledgedAt      The timestamp of acknowledgment (UTC)
     * @param applicationVersion  The version of the application
     * @return true if saved successfully, false otherwise
     */
    public abstract boolean saveAcknowledgment(String privacyVersion, Instant acknowledgedAt, String applicationVersion);

    /**
     * Gets the version of the privacy notice that was last acknowledged.
     *
     * @return Optional containing the acknowledged version, or empty if never acknowledged
     */
    public abstract Optional<String> getAcknowledgedVersion();

    /**
     * Checks if the user has acknowledged the specified privacy notice version.
     *
     * @param version The privacy notice version to check
     * @return true if the user has acknowledged this version, false otherwise
     */
    public boolean hasAcknowledgedVersion(String version) {
        return getAcknowledgedVersion()
                .map(v -> v.equals(version))
                .orElse(false);
    }

    /**
     * Gets the timestamp of the last acknowledgment.
     *
     * @return Optional containing the acknowledgment timestamp, or empty if never acknowledged
     */
    public abstract Optional<Instant> getAcknowledgmentTimestamp();
}
