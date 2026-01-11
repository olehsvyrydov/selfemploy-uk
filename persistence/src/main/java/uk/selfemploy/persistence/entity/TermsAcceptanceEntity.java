package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for Terms of Service Acceptance.
 *
 * SE-508: Terms of Service UI
 * Stores user acceptances of Terms of Service with version tracking.
 * Includes scroll completion timestamp for legal evidence.
 */
@Entity
@Table(name = "terms_acceptance", indexes = {
    @Index(name = "idx_tos_version", columnList = "tos_version"),
    @Index(name = "idx_tos_accepted_at", columnList = "accepted_at DESC")
})
public class TermsAcceptanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tos_version", nullable = false, length = 20)
    private String tosVersion;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "scroll_completed_at", nullable = false)
    private Instant scrollCompletedAt;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "application_version", nullable = false, length = 50)
    private String applicationVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    // Default constructor for JPA
    public TermsAcceptanceEntity() {
    }

    /**
     * Creates a new terms acceptance entity.
     *
     * @param tosVersion          The version of the ToS being accepted
     * @param acceptedAt          The timestamp of acceptance (UTC)
     * @param scrollCompletedAt   The timestamp when user scrolled to bottom (UTC)
     * @param applicationVersion  The version of the application
     */
    public TermsAcceptanceEntity(String tosVersion, Instant acceptedAt, Instant scrollCompletedAt, String applicationVersion) {
        this.tosVersion = tosVersion;
        this.acceptedAt = acceptedAt;
        this.scrollCompletedAt = scrollCompletedAt;
        this.applicationVersion = applicationVersion;
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTosVersion() {
        return tosVersion;
    }

    public void setTosVersion(String tosVersion) {
        this.tosVersion = tosVersion;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Instant getScrollCompletedAt() {
        return scrollCompletedAt;
    }

    public void setScrollCompletedAt(Instant scrollCompletedAt) {
        this.scrollCompletedAt = scrollCompletedAt;
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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
