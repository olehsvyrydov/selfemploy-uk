package uk.selfemploy.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.persistence.repository.PrivacyAcknowledgmentRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing privacy notice acknowledgments.
 *
 * SE-507: Privacy Notice UI
 * Handles storage and retrieval of user privacy acknowledgments with version tracking.
 */
@ApplicationScoped
public class PrivacyAcknowledgmentService {

    private final PrivacyAcknowledgmentRepository repository;

    @Inject
    public PrivacyAcknowledgmentService(PrivacyAcknowledgmentRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves a privacy notice acknowledgment.
     *
     * @param privacyVersion      The version of the privacy notice being acknowledged
     * @param acknowledgedAt      The timestamp of acknowledgment (UTC)
     * @param applicationVersion  The version of the application
     * @return true if saved successfully, false otherwise
     */
    public boolean saveAcknowledgment(String privacyVersion, Instant acknowledgedAt, String applicationVersion) {
        if (privacyVersion == null || privacyVersion.isBlank()) {
            return false;
        }
        if (acknowledgedAt == null) {
            return false;
        }
        if (applicationVersion == null || applicationVersion.isBlank()) {
            return false;
        }

        return repository.save(privacyVersion, acknowledgedAt, applicationVersion);
    }

    /**
     * Gets the version of the privacy notice that was last acknowledged.
     *
     * @return Optional containing the acknowledged version, or empty if never acknowledged
     */
    public Optional<String> getAcknowledgedVersion() {
        return repository.getLatestAcknowledgedVersion();
    }

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
    public Optional<Instant> getAcknowledgmentTimestamp() {
        return repository.getLatestAcknowledgmentTimestamp();
    }
}
