package uk.selfemploy.plugin.runtime;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a revoked certificate entry in the revocation list.
 *
 * <p>Each entry contains:</p>
 * <ul>
 *   <li>The certificate fingerprint (SHA-256 hash)</li>
 *   <li>The reason for revocation</li>
 *   <li>The timestamp when the certificate was revoked</li>
 * </ul>
 *
 * <h2>Fingerprint Format</h2>
 * <p>The fingerprint is in the format {@code sha256:<hex>} where {@code <hex>}
 * is the lowercase hexadecimal representation of the SHA-256 hash of the
 * certificate's DER-encoded bytes.</p>
 *
 * @param fingerprint the certificate fingerprint (e.g., "sha256:abc123...")
 * @param reason      the reason for revocation (e.g., "Key compromise")
 * @param revokedAt   the timestamp when the certificate was revoked
 */
public record RevokedCertificateEntry(
    String fingerprint,
    String reason,
    Instant revokedAt
) {
    /**
     * Creates a new revoked certificate entry.
     *
     * @param fingerprint the certificate fingerprint (must start with "sha256:")
     * @param reason      the reason for revocation
     * @param revokedAt   the timestamp when revoked
     * @throws NullPointerException     if fingerprint or revokedAt is null
     * @throws IllegalArgumentException if fingerprint doesn't start with "sha256:"
     */
    public RevokedCertificateEntry {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(revokedAt, "revokedAt must not be null");

        // Normalize fingerprint to lowercase
        fingerprint = fingerprint.toLowerCase();

        if (!fingerprint.startsWith("sha256:")) {
            throw new IllegalArgumentException(
                "fingerprint must start with 'sha256:', was: " + fingerprint
            );
        }
    }

    /**
     * Returns the fingerprint in normalized (lowercase) form.
     *
     * @return the normalized fingerprint
     */
    @Override
    public String fingerprint() {
        return fingerprint;
    }

    /**
     * Checks if this entry matches the given fingerprint.
     *
     * <p>Comparison is case-insensitive.</p>
     *
     * @param otherFingerprint the fingerprint to compare
     * @return true if the fingerprints match
     */
    public boolean matches(String otherFingerprint) {
        if (otherFingerprint == null) {
            return false;
        }
        return fingerprint.equalsIgnoreCase(otherFingerprint);
    }

    @Override
    public String toString() {
        return String.format(
            "RevokedCertificateEntry[fingerprint=%s, reason=%s, revokedAt=%s]",
            fingerprint, reason, revokedAt
        );
    }
}
