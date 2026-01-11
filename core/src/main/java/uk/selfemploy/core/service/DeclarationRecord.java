package uk.selfemploy.core.service;

import java.time.Instant;

/**
 * Immutable record representing a declaration acceptance event.
 *
 * <p>Used to capture when a user accepted the HMRC declaration, including
 * the exact UTC timestamp and a hash of the declaration text for version tracking.</p>
 *
 * @param acceptedAt          UTC timestamp when the declaration was accepted
 * @param declarationTextHash SHA-256 hash of the declaration text (64 hex chars)
 * @param declarationText     The actual declaration text that was accepted
 */
public record DeclarationRecord(
        Instant acceptedAt,
        String declarationTextHash,
        String declarationText
) {

    /**
     * Compact constructor for validation.
     */
    public DeclarationRecord {
        if (acceptedAt == null) {
            throw new IllegalArgumentException("Acceptance timestamp is required");
        }
        if (declarationTextHash == null || declarationTextHash.isBlank()) {
            throw new IllegalArgumentException("Declaration text hash is required");
        }
        if (declarationText == null || declarationText.isBlank()) {
            throw new IllegalArgumentException("Declaration text is required");
        }
    }
}
