package uk.selfemploy.core.service;

import java.time.Instant;
import java.util.Optional;

/**
 * Service contract for managing Terms of Service acceptances.
 *
 * <p>Handles storage and retrieval of user ToS acceptances with version tracking, including a
 * scroll completion timestamp for legal evidence. Persistence is supplied by concrete subclasses
 * (the desktop app's SQLite-backed implementation), so this type carries no storage dependency.</p>
 */
public abstract class TermsAcceptanceService {

    protected TermsAcceptanceService() {
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
    public abstract boolean saveAcceptance(String tosVersion, Instant acceptedAt, Instant scrollCompletedAt, String applicationVersion);

    /**
     * Gets the version of the ToS that was last accepted.
     *
     * @return Optional containing the accepted version, or empty if never accepted
     */
    public abstract Optional<String> getAcceptedVersion();

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
    public abstract Optional<Instant> getAcceptanceTimestamp();

    /**
     * Gets the scroll completed timestamp of the last acceptance.
     *
     * @return Optional containing the scroll completed timestamp, or empty if never accepted
     */
    public abstract Optional<Instant> getScrollCompletedTimestamp();
}
