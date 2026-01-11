package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for Privacy Notice Acknowledgment.
 *
 * SE-507: Privacy Notice UI
 * Stores user acknowledgments of privacy notices with version tracking.
 */
@Entity
@Table(name = "privacy_acknowledgment", indexes = {
    @Index(name = "idx_privacy_version", columnList = "privacy_notice_version"),
    @Index(name = "idx_privacy_acknowledged_at", columnList = "acknowledged_at DESC")
})
public class PrivacyAcknowledgmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "privacy_notice_version", nullable = false, length = 20)
    private String privacyNoticeVersion;

    @Column(name = "acknowledged_at", nullable = false)
    private Instant acknowledgedAt;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "application_version", nullable = false, length = 50)
    private String applicationVersion;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    // Default constructor for JPA
    public PrivacyAcknowledgmentEntity() {
    }

    /**
     * Creates a new privacy acknowledgment entity.
     *
     * @param privacyNoticeVersion The version of the privacy notice being acknowledged
     * @param acknowledgedAt       The timestamp of acknowledgment (UTC)
     * @param applicationVersion   The version of the application
     */
    public PrivacyAcknowledgmentEntity(String privacyNoticeVersion, Instant acknowledgedAt, String applicationVersion) {
        this.privacyNoticeVersion = privacyNoticeVersion;
        this.acknowledgedAt = acknowledgedAt;
        this.applicationVersion = applicationVersion;
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPrivacyNoticeVersion() {
        return privacyNoticeVersion;
    }

    public void setPrivacyNoticeVersion(String privacyNoticeVersion) {
        this.privacyNoticeVersion = privacyNoticeVersion;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
