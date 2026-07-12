package uk.selfemploy.ui.service;

import java.time.Instant;
import java.util.Optional;

/**
 * Port for persisting and reading Terms of Service acceptances.
 *
 * <p>The shipping desktop app implements this with {@link SqliteTermsAcceptanceRepository}
 * (a JDBC adapter). Keeping it an interface lets callers depend on the port rather than the
 * SQLite adapter, and lets a future edition supply a different backing store.</p>
 */
public interface TermsAcceptanceRepository {

    /**
     * Saves a new terms acceptance.
     *
     * @return true if saved successfully, false otherwise
     */
    boolean save(String tosVersion, Instant acceptedAt, Instant scrollCompletedAt, String applicationVersion);

    /**
     * Returns the most recently accepted ToS version, or empty if none.
     */
    Optional<String> getLatestAcceptedVersion();

    /**
     * Returns the timestamp of the most recent acceptance, or empty if none.
     */
    Optional<Instant> getLatestAcceptanceTimestamp();

    /**
     * Returns the scroll-completed timestamp of the most recent acceptance, or empty if none.
     */
    Optional<Instant> getLatestScrollCompletedTimestamp();
}
