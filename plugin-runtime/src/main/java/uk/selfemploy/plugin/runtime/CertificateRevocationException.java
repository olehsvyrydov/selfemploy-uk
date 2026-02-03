package uk.selfemploy.plugin.runtime;

/**
 * Exception thrown when certificate revocation checking fails.
 *
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>The revocation list file cannot be parsed</li>
 *   <li>The revocation list has invalid format</li>
 *   <li>A certificate is found to be revoked</li>
 * </ul>
 */
public class CertificateRevocationException extends PluginException {

    private final String fingerprint;
    private final String reason;

    /**
     * Creates a new exception for a parsing error.
     *
     * @param message the error message
     */
    public CertificateRevocationException(String message) {
        super(message);
        this.fingerprint = null;
        this.reason = null;
    }

    /**
     * Creates a new exception for a parsing error with a cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public CertificateRevocationException(String message, Throwable cause) {
        super(message, cause);
        this.fingerprint = null;
        this.reason = null;
    }

    /**
     * Creates a new exception for a revoked certificate.
     *
     * @param fingerprint the certificate fingerprint
     * @param reason      the revocation reason
     */
    public CertificateRevocationException(String fingerprint, String reason) {
        super(buildRevokedMessage(fingerprint, reason));
        this.fingerprint = fingerprint;
        this.reason = reason;
    }

    private static String buildRevokedMessage(String fingerprint, String reason) {
        return String.format(
            "Certificate has been revoked. Fingerprint: %s, Reason: %s",
            fingerprint,
            reason != null ? reason : "Unknown"
        );
    }

    /**
     * Returns the fingerprint of the revoked certificate.
     *
     * @return the fingerprint, or null if this is not a revocation exception
     */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * Returns the revocation reason.
     *
     * @return the reason, or null if this is not a revocation exception
     */
    public String getReason() {
        return reason;
    }

    /**
     * Checks if this exception is for a revoked certificate (vs a parsing error).
     *
     * @return true if a certificate was revoked
     */
    public boolean isRevocationError() {
        return fingerprint != null;
    }
}
