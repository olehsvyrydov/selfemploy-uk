package uk.selfemploy.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Service for managing HMRC declaration acceptance.
 *
 * <p>Handles recording when users accept the HMRC declaration, generating
 * hash values for declaration text version tracking, and validating declaration
 * timestamps for compliance.</p>
 *
 * <p>This service ensures that:</p>
 * <ul>
 *   <li>Declaration acceptance is recorded with UTC timestamps</li>
 *   <li>Declaration text is hashed (SHA-256) for version tracking</li>
 *   <li>Timestamps can be validated before submission</li>
 * </ul>
 *
 * @see DeclarationRecord
 */
@ApplicationScoped
public class DeclarationService {

    /**
     * Standard HMRC Self Assessment declaration text.
     *
     * <p>This is the legally binding declaration that taxpayers must accept
     * before submitting their Self Assessment return.</p>
     */
    private static final String HMRC_DECLARATION_TEXT =
            "I declare that the information I have given on this return is correct and complete " +
            "to the best of my knowledge and belief. I understand that I may have to pay financial " +
            "penalties and face prosecution if I give false information.";

    /**
     * Maximum age of a declaration timestamp (24 hours).
     * Declarations older than this are considered invalid.
     */
    private static final Duration MAX_DECLARATION_AGE = Duration.ofHours(24);

    private final Clock clock;

    /**
     * Creates a DeclarationService with the system UTC clock.
     */
    public DeclarationService() {
        this(Clock.systemUTC());
    }

    /**
     * Creates a DeclarationService with a specific clock.
     * Primarily used for testing with fixed clocks.
     *
     * @param clock the clock to use for timestamps
     */
    @Inject
    public DeclarationService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Returns the standard HMRC declaration text.
     *
     * @return the declaration text that users must accept
     */
    public String getDeclarationText() {
        return HMRC_DECLARATION_TEXT;
    }

    /**
     * Records a declaration acceptance with the current UTC timestamp.
     *
     * <p>This method should be called when the user checks the declaration
     * checkbox in the UI. It creates an immutable record of the acceptance.</p>
     *
     * @return a DeclarationRecord containing the timestamp, hash, and text
     */
    public DeclarationRecord recordDeclarationAcceptance() {
        Instant acceptedAt = clock.instant();
        String hash = generateDeclarationTextHash();
        return new DeclarationRecord(acceptedAt, hash, HMRC_DECLARATION_TEXT);
    }

    /**
     * Generates a SHA-256 hash of the current declaration text.
     *
     * <p>The hash allows detecting if the declaration text has changed
     * between when a submission was made and now. This is important for
     * compliance and audit trail purposes.</p>
     *
     * @return 64-character lowercase hex string representing the SHA-256 hash
     */
    public String generateDeclarationTextHash() {
        return hashString(HMRC_DECLARATION_TEXT);
    }

    /**
     * Validates that a declaration timestamp is acceptable for submission.
     *
     * <p>A valid declaration timestamp must:</p>
     * <ul>
     *   <li>Not be null</li>
     *   <li>Not be in the future</li>
     *   <li>Not be older than 24 hours</li>
     * </ul>
     *
     * @param timestamp the declaration acceptance timestamp to validate
     * @return true if the timestamp is valid, false otherwise
     */
    public boolean isValidDeclarationTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return false;
        }

        Instant now = clock.instant();

        // Reject future timestamps
        if (timestamp.isAfter(now)) {
            return false;
        }

        // Reject timestamps older than 24 hours
        Instant oldestAllowed = now.minus(MAX_DECLARATION_AGE);
        if (timestamp.isBefore(oldestAllowed)) {
            return false;
        }

        return true;
    }

    /**
     * Verifies that a hash matches the current declaration text hash.
     *
     * <p>This allows checking whether a submission was made with the
     * same declaration text as is currently in use.</p>
     *
     * @param hash the hash to verify
     * @return true if the hash matches the current declaration text, false otherwise
     */
    public boolean verifyDeclarationHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return false;
        }

        // Validate hash format (64 lowercase hex chars)
        if (!hash.matches("[a-f0-9]{64}")) {
            return false;
        }

        String currentHash = generateDeclarationTextHash();
        return currentHash.equals(hash);
    }

    /**
     * Computes SHA-256 hash of a string.
     *
     * @param input the string to hash
     * @return 64-character lowercase hex string
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts byte array to lowercase hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
