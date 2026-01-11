package uk.selfemploy.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.persistence.repository.TermsAcceptanceRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing Terms of Service acceptances.
 *
 * SE-508: Terms of Service UI
 * Handles storage and retrieval of user ToS acceptances with version tracking.
 * Includes scroll completion timestamp for legal evidence.
 */
@ApplicationScoped
public class TermsAcceptanceService {

    private final TermsAcceptanceRepository repository;

    @Inject
    public TermsAcceptanceService(TermsAcceptanceRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves a Terms of Service acceptance.
     *
     * @param tosVersion          The version of the ToS being accepted
     * @param acceptedAt          The timestamp of acceptance (UTC)
     * @param scrollCompletedAt   The timestamp when user scrolled to bottom (UTC)
     * @param applicationVersion  The version of the application
     * @return true if saved successfully, false otherwise
     */
    public boolean saveAcceptance(String tosVersion, Instant acceptedAt, Instant scrollCompletedAt, String applicationVersion) {
        if (tosVersion == null || tosVersion.isBlank()) {
            return false;
        }
        if (acceptedAt == null) {
            return false;
        }
        if (scrollCompletedAt == null) {
            return false;
        }
        if (applicationVersion == null || applicationVersion.isBlank()) {
            return false;
        }

        return repository.save(tosVersion, acceptedAt, scrollCompletedAt, applicationVersion);
    }

    /**
     * Gets the version of the ToS that was last accepted.
     *
     * @return Optional containing the accepted version, or empty if never accepted
     */
    public Optional<String> getAcceptedVersion() {
        return repository.getLatestAcceptedVersion();
    }

    /**
     * Checks if the user has accepted the specified ToS version.
     *
     * @param version The ToS version to check
     * @return true if the user has accepted this version, false otherwise
     */
    public boolean hasAcceptedVersion(String version) {
        return getAcceptedVersion()
                .map(v -> v.equals(version))
                .orElse(false);
    }

    /**
     * Gets the timestamp of the last acceptance.
     *
     * @return Optional containing the acceptance timestamp, or empty if never accepted
     */
    public Optional<Instant> getAcceptanceTimestamp() {
        return repository.getLatestAcceptanceTimestamp();
    }

    /**
     * Gets the scroll completed timestamp of the last acceptance.
     *
     * @return Optional containing the scroll completed timestamp, or empty if never accepted
     */
    public Optional<Instant> getScrollCompletedTimestamp() {
        return repository.getLatestScrollCompletedTimestamp();
    }
}
