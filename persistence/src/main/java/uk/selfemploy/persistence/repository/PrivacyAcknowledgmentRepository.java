package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.persistence.entity.PrivacyAcknowledgmentEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Privacy Acknowledgment entities.
 *
 * SE-507: Privacy Notice UI
 * Provides persistence operations for privacy notice acknowledgments.
 */
@ApplicationScoped
public class PrivacyAcknowledgmentRepository implements PanacheRepositoryBase<PrivacyAcknowledgmentEntity, UUID> {

    /**
     * Saves a new privacy acknowledgment.
     *
     * @param privacyVersion     The version of the privacy notice
     * @param acknowledgedAt     The timestamp of acknowledgment
     * @param applicationVersion The application version
     * @return true if saved successfully, false otherwise
     */
    public boolean save(String privacyVersion, Instant acknowledgedAt, String applicationVersion) {
        try {
            PrivacyAcknowledgmentEntity entity = new PrivacyAcknowledgmentEntity(
                privacyVersion,
                acknowledgedAt,
                applicationVersion
            );
            persist(entity);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the most recently acknowledged privacy notice version.
     *
     * @return Optional containing the version string, or empty if no acknowledgments exist
     */
    public Optional<String> getLatestAcknowledgedVersion() {
        return find("ORDER BY acknowledgedAt DESC")
            .firstResultOptional()
            .map(PrivacyAcknowledgmentEntity::getPrivacyNoticeVersion);
    }

    /**
     * Gets the timestamp of the most recent acknowledgment.
     *
     * @return Optional containing the timestamp, or empty if no acknowledgments exist
     */
    public Optional<Instant> getLatestAcknowledgmentTimestamp() {
        return find("ORDER BY acknowledgedAt DESC")
            .firstResultOptional()
            .map(PrivacyAcknowledgmentEntity::getAcknowledgedAt);
    }

    /**
     * Checks if a specific version has been acknowledged.
     *
     * @param version The privacy notice version to check
     * @return true if the version has been acknowledged, false otherwise
     */
    public boolean hasAcknowledgedVersion(String version) {
        return count("privacyNoticeVersion", version) > 0;
    }

    /**
     * Gets all acknowledgments ordered by date (most recent first).
     *
     * @return List of all acknowledgment entities
     */
    public java.util.List<PrivacyAcknowledgmentEntity> findAllOrderedByDate() {
        return list("ORDER BY acknowledgedAt DESC");
    }

    /**
     * Deletes all acknowledgments (for testing purposes).
     */
    public void deleteAllAcknowledgments() {
        deleteAll();
    }
}
