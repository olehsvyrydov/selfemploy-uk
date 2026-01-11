package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.persistence.entity.TermsAcceptanceEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Terms Acceptance entities.
 *
 * SE-508: Terms of Service UI
 * Provides persistence operations for Terms of Service acceptances.
 */
@ApplicationScoped
public class TermsAcceptanceRepository implements PanacheRepositoryBase<TermsAcceptanceEntity, UUID> {

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
        try {
            TermsAcceptanceEntity entity = new TermsAcceptanceEntity(
                tosVersion,
                acceptedAt,
                scrollCompletedAt,
                applicationVersion
            );
            persist(entity);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the most recently accepted ToS version.
     *
     * @return Optional containing the version string, or empty if no acceptances exist
     */
    public Optional<String> getLatestAcceptedVersion() {
        return find("ORDER BY acceptedAt DESC")
            .firstResultOptional()
            .map(TermsAcceptanceEntity::getTosVersion);
    }

    /**
     * Gets the timestamp of the most recent acceptance.
     *
     * @return Optional containing the timestamp, or empty if no acceptances exist
     */
    public Optional<Instant> getLatestAcceptanceTimestamp() {
        return find("ORDER BY acceptedAt DESC")
            .firstResultOptional()
            .map(TermsAcceptanceEntity::getAcceptedAt);
    }

    /**
     * Gets the scroll completed timestamp of the most recent acceptance.
     *
     * @return Optional containing the timestamp, or empty if no acceptances exist
     */
    public Optional<Instant> getLatestScrollCompletedTimestamp() {
        return find("ORDER BY acceptedAt DESC")
            .firstResultOptional()
            .map(TermsAcceptanceEntity::getScrollCompletedAt);
    }

    /**
     * Checks if a specific version has been accepted.
     *
     * @param version The ToS version to check
     * @return true if the version has been accepted, false otherwise
     */
    public boolean hasAcceptedVersion(String version) {
        return count("tosVersion", version) > 0;
    }

    /**
     * Gets all acceptances ordered by date (most recent first).
     *
     * @return List of all acceptance entities
     */
    public java.util.List<TermsAcceptanceEntity> findAllOrderedByDate() {
        return list("ORDER BY acceptedAt DESC");
    }

    /**
     * Gets the most recent acceptance entity.
     *
     * @return Optional containing the entity, or empty if no acceptances exist
     */
    public Optional<TermsAcceptanceEntity> getLatestAcceptance() {
        return find("ORDER BY acceptedAt DESC")
            .firstResultOptional();
    }

    /**
     * Deletes all acceptances (for testing purposes).
     */
    public void deleteAllAcceptances() {
        deleteAll();
    }
}
