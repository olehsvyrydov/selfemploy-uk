package uk.selfemploy.plugin.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Certificate Revocation List (CRL) for plugin signature verification.
 *
 * <p>This class manages a list of revoked certificates that should be rejected
 * during plugin signature verification. The revocation list is stored as a JSON
 * file in the application's configuration directory.</p>
 *
 * <h2>JSON Format</h2>
 * <pre>{@code
 * {
 *   "version": 1,
 *   "updated": "2026-02-01T00:00:00Z",
 *   "revoked": [
 *     {
 *       "fingerprint": "sha256:abc123...",
 *       "reason": "Key compromise",
 *       "revokedAt": "2026-01-15T00:00:00Z"
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Load from file (returns empty list if file doesn't exist)
 * CertificateRevocationList crl = CertificateRevocationList.load(configDir.resolve("revocation-list.json"));
 *
 * // Check if a certificate is revoked
 * byte[] certBytes = ...; // DER-encoded certificate
 * String fingerprint = CertificateRevocationList.computeFingerprint(certBytes);
 * if (crl.isRevoked(fingerprint)) {
 *     RevokedCertificateEntry entry = crl.getRevocationEntry(fingerprint).orElseThrow();
 *     throw new CertificateRevocationException(entry.fingerprint(), entry.reason());
 * }
 * }</pre>
 *
 * <h2>Security Notes</h2>
 * <ul>
 *   <li>Revocation check should happen BEFORE signature verification</li>
 *   <li>Fingerprints use SHA-256 for collision resistance</li>
 *   <li>Comparison is case-insensitive for robustness</li>
 * </ul>
 *
 * @see RevokedCertificateEntry
 * @see CertificateRevocationException
 */
public class CertificateRevocationList {

    private static final ObjectMapper MAPPER = createObjectMapper();

    private final int version;
    private final Instant updated;
    private final List<RevokedCertificateEntry> revokedEntries;

    /**
     * Private constructor - use {@link #load(Path)}, {@link #empty()}, or {@link #builder()}.
     */
    private CertificateRevocationList(int version, Instant updated, List<RevokedCertificateEntry> revokedEntries) {
        this.version = version;
        this.updated = updated;
        this.revokedEntries = Collections.unmodifiableList(new ArrayList<>(revokedEntries));
    }

    /**
     * Loads a certificate revocation list from a JSON file.
     *
     * <p>If the file does not exist, returns an empty revocation list.
     * This allows the application to work without a revocation list file
     * (no certificates will be considered revoked).</p>
     *
     * @param path the path to the revocation list JSON file
     * @return the loaded revocation list, or empty list if file doesn't exist
     * @throws CertificateRevocationException if the file exists but cannot be parsed
     */
    public static CertificateRevocationList load(Path path) {
        Objects.requireNonNull(path, "path must not be null");

        if (!Files.exists(path)) {
            return empty();
        }

        try {
            JsonModel model = MAPPER.readValue(path.toFile(), JsonModel.class);
            return model.toCertificateRevocationList();
        } catch (IOException e) {
            throw new CertificateRevocationException("Failed to parse revocation list: " + path, e);
        }
    }

    /**
     * Creates an empty certificate revocation list.
     *
     * <p>An empty list has version 0 and no revoked entries.</p>
     *
     * @return an empty revocation list
     */
    public static CertificateRevocationList empty() {
        return new CertificateRevocationList(0, Instant.EPOCH, Collections.emptyList());
    }

    /**
     * Creates a new builder for constructing a certificate revocation list.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Computes a SHA-256 fingerprint for the given certificate bytes.
     *
     * <p>The fingerprint is in the format {@code sha256:<hex>} where {@code <hex>}
     * is the lowercase hexadecimal representation of the SHA-256 hash.</p>
     *
     * @param certBytes the DER-encoded certificate bytes
     * @return the fingerprint string (e.g., "sha256:abc123...")
     * @throws NullPointerException if certBytes is null
     */
    public static String computeFingerprint(byte[] certBytes) {
        Objects.requireNonNull(certBytes, "certBytes must not be null");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(certBytes);
            String hex = HexFormat.of().formatHex(hash);
            return "sha256:" + hex;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in all Java implementations
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Saves this revocation list to a JSON file.
     *
     * @param path the path to save to
     * @throws IOException if the file cannot be written
     */
    public void save(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");

        JsonModel model = JsonModel.fromCertificateRevocationList(this);
        MAPPER.writeValue(path.toFile(), model);
    }

    /**
     * Checks if a certificate with the given fingerprint is revoked.
     *
     * <p>Comparison is case-insensitive.</p>
     *
     * @param fingerprint the certificate fingerprint to check
     * @return true if the certificate is revoked
     */
    public boolean isRevoked(String fingerprint) {
        return getRevocationEntry(fingerprint).isPresent();
    }

    /**
     * Gets the revocation entry for a certificate, if revoked.
     *
     * <p>Comparison is case-insensitive.</p>
     *
     * @param fingerprint the certificate fingerprint to look up
     * @return the revocation entry, or empty if not revoked
     */
    public Optional<RevokedCertificateEntry> getRevocationEntry(String fingerprint) {
        if (fingerprint == null) {
            return Optional.empty();
        }

        return revokedEntries.stream()
            .filter(entry -> entry.matches(fingerprint))
            .findFirst();
    }

    /**
     * Returns the version number of this revocation list.
     *
     * @return the version number
     */
    public int getVersion() {
        return version;
    }

    /**
     * Returns the timestamp when this revocation list was last updated.
     *
     * @return the update timestamp
     */
    public Instant getUpdated() {
        return updated;
    }

    /**
     * Returns an unmodifiable list of all revoked certificate entries.
     *
     * @return the list of revoked entries
     */
    public List<RevokedCertificateEntry> getRevokedEntries() {
        return revokedEntries;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Builder for creating {@link CertificateRevocationList} instances.
     */
    public static class Builder {
        private int version = 1;
        private Instant updated = Instant.now();
        private final List<RevokedCertificateEntry> entries = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the version number.
         *
         * @param version the version number
         * @return this builder
         */
        public Builder version(int version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the update timestamp.
         *
         * @param updated the update timestamp
         * @return this builder
         */
        public Builder updated(Instant updated) {
            this.updated = Objects.requireNonNull(updated, "updated must not be null");
            return this;
        }

        /**
         * Adds a revoked certificate entry.
         *
         * @param entry the entry to add
         * @return this builder
         */
        public Builder addRevoked(RevokedCertificateEntry entry) {
            this.entries.add(Objects.requireNonNull(entry, "entry must not be null"));
            return this;
        }

        /**
         * Builds the certificate revocation list.
         *
         * @return the built revocation list
         */
        public CertificateRevocationList build() {
            return new CertificateRevocationList(version, updated, entries);
        }
    }

    /**
     * Internal JSON model for serialization/deserialization.
     */
    private static class JsonModel {
        @JsonProperty("version")
        Integer version;

        @JsonProperty("updated")
        Instant updated;

        @JsonProperty("revoked")
        List<RevokedEntryModel> revoked;

        static JsonModel fromCertificateRevocationList(CertificateRevocationList crl) {
            JsonModel model = new JsonModel();
            model.version = crl.getVersion();
            model.updated = crl.getUpdated();
            model.revoked = crl.getRevokedEntries().stream()
                .map(RevokedEntryModel::fromEntry)
                .toList();
            return model;
        }

        CertificateRevocationList toCertificateRevocationList() {
            if (version == null) {
                throw new CertificateRevocationException("Missing required field: version");
            }

            List<RevokedCertificateEntry> entries = revoked != null
                ? revoked.stream().map(RevokedEntryModel::toEntry).toList()
                : Collections.emptyList();

            return new CertificateRevocationList(
                version,
                updated != null ? updated : Instant.EPOCH,
                entries
            );
        }
    }

    /**
     * Internal JSON model for revoked entry serialization.
     */
    private static class RevokedEntryModel {
        @JsonProperty("fingerprint")
        String fingerprint;

        @JsonProperty("reason")
        String reason;

        @JsonProperty("revokedAt")
        Instant revokedAt;

        static RevokedEntryModel fromEntry(RevokedCertificateEntry entry) {
            RevokedEntryModel model = new RevokedEntryModel();
            model.fingerprint = entry.fingerprint();
            model.reason = entry.reason();
            model.revokedAt = entry.revokedAt();
            return model;
        }

        RevokedCertificateEntry toEntry() {
            return new RevokedCertificateEntry(fingerprint, reason, revokedAt);
        }
    }
}
