package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CertificateRevocationList}.
 *
 * <p>Tests verify revocation list functionality including:</p>
 * <ul>
 *   <li>Loading from JSON file in config directory</li>
 *   <li>SHA-256 fingerprint format</li>
 *   <li>Checking if certificates are revoked</li>
 *   <li>Clear error messages with revocation details</li>
 * </ul>
 */
@DisplayName("CertificateRevocationList")
class CertificateRevocationListTest {

    @TempDir
    Path tempDir;

    private Path revocationListPath;

    @BeforeEach
    void setUp() {
        revocationListPath = tempDir.resolve("revocation-list.json");
    }

    @Nested
    @DisplayName("Loading from file")
    class LoadingTests {

        @Test
        @DisplayName("should load valid revocation list from JSON file")
        void shouldLoadValidRevocationList() throws IOException {
            // Given
            String json = """
                {
                  "version": 1,
                  "updated": "2026-02-01T00:00:00Z",
                  "revoked": [
                    {
                      "fingerprint": "sha256:abc123def456",
                      "reason": "Key compromise",
                      "revokedAt": "2026-01-15T00:00:00Z"
                    }
                  ]
                }
                """;
            Files.writeString(revocationListPath, json);

            // When
            CertificateRevocationList crl = CertificateRevocationList.load(revocationListPath);

            // Then
            assertThat(crl.getVersion()).isEqualTo(1);
            assertThat(crl.getUpdated()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
            assertThat(crl.getRevokedEntries()).hasSize(1);
        }

        @Test
        @DisplayName("should load revocation list with multiple entries")
        void shouldLoadMultipleEntries() throws IOException {
            // Given
            String json = """
                {
                  "version": 1,
                  "updated": "2026-02-01T00:00:00Z",
                  "revoked": [
                    {
                      "fingerprint": "sha256:abc123",
                      "reason": "Key compromise",
                      "revokedAt": "2026-01-15T00:00:00Z"
                    },
                    {
                      "fingerprint": "sha256:def456",
                      "reason": "Certificate superseded",
                      "revokedAt": "2026-01-20T00:00:00Z"
                    }
                  ]
                }
                """;
            Files.writeString(revocationListPath, json);

            // When
            CertificateRevocationList crl = CertificateRevocationList.load(revocationListPath);

            // Then
            assertThat(crl.getRevokedEntries()).hasSize(2);
        }

        @Test
        @DisplayName("should handle empty revocation list")
        void shouldHandleEmptyRevocationList() throws IOException {
            // Given
            String json = """
                {
                  "version": 1,
                  "updated": "2026-02-01T00:00:00Z",
                  "revoked": []
                }
                """;
            Files.writeString(revocationListPath, json);

            // When
            CertificateRevocationList crl = CertificateRevocationList.load(revocationListPath);

            // Then
            assertThat(crl.getRevokedEntries()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when file does not exist")
        void shouldReturnEmptyWhenFileNotExists() {
            // Given
            Path nonExistent = tempDir.resolve("nonexistent.json");

            // When
            CertificateRevocationList crl = CertificateRevocationList.load(nonExistent);

            // Then
            assertThat(crl.getRevokedEntries()).isEmpty();
            assertThat(crl.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw on invalid JSON")
        void shouldThrowOnInvalidJson() throws IOException {
            // Given
            Files.writeString(revocationListPath, "not valid json");

            // When/Then
            assertThatThrownBy(() -> CertificateRevocationList.load(revocationListPath))
                .isInstanceOf(CertificateRevocationException.class)
                .hasMessageContaining("Failed to parse revocation list");
        }

        @Test
        @DisplayName("should throw on missing required fields")
        void shouldThrowOnMissingRequiredFields() throws IOException {
            // Given - missing version
            String json = """
                {
                  "updated": "2026-02-01T00:00:00Z",
                  "revoked": []
                }
                """;
            Files.writeString(revocationListPath, json);

            // When/Then
            assertThatThrownBy(() -> CertificateRevocationList.load(revocationListPath))
                .isInstanceOf(CertificateRevocationException.class)
                .hasMessageContaining("version");
        }
    }

    @Nested
    @DisplayName("Checking revocation status")
    class RevocationCheckTests {

        @Test
        @DisplayName("should return true for revoked fingerprint")
        void shouldReturnTrueForRevokedFingerprint() throws IOException {
            // Given
            String json = """
                {
                  "version": 1,
                  "updated": "2026-02-01T00:00:00Z",
                  "revoked": [
                    {
                      "fingerprint": "sha256:abc123def456",
                      "reason": "Key compromise",
                      "revokedAt": "2026-01-15T00:00:00Z"
                    }
                  ]
                }
                """;
            Files.writeString(revocationListPath, json);
            CertificateRevocationList crl = CertificateRevocationList.load(revocationListPath);

            // When/Then
            assertThat(crl.isRevoked("sha256:abc123def456")).isTrue();
        }

        @Test
        @DisplayName("should return false for non-revoked fingerprint")
        void shouldReturnFalseForNonRevokedFingerprint() throws IOException {
            // Given
            String json = """
                {
                  "version": 1,
                  "updated": "2026-02-01T00:00:00Z",
                  "revoked": [
                    {
                      "fingerprint": "sha256:abc123def456",
                      "reason": "Key compromise",
                      "revokedAt": "2026-01-15T00:00:00Z"
                    }
                  ]
                }
                """;
            Files.writeString(revocationListPath, json);
            CertificateRevocationList crl = CertificateRevocationList.load(revocationListPath);

            // When/Then
            assertThat(crl.isRevoked("sha256:xyz789")).isFalse();
        }

        @Test
        @DisplayName("should be case-insensitive for fingerprint lookup")
        void shouldBeCaseInsensitive() throws IOException {
            // Given
            String json = """
                {
                  "version": 1,
                  "updated": "2026-02-01T00:00:00Z",
                  "revoked": [
                    {
                      "fingerprint": "sha256:ABC123DEF456",
                      "reason": "Key compromise",
                      "revokedAt": "2026-01-15T00:00:00Z"
                    }
                  ]
                }
                """;
            Files.writeString(revocationListPath, json);
            CertificateRevocationList crl = CertificateRevocationList.load(revocationListPath);

            // When/Then
            assertThat(crl.isRevoked("sha256:abc123def456")).isTrue();
            assertThat(crl.isRevoked("SHA256:ABC123DEF456")).isTrue();
        }

        @Test
        @DisplayName("should get revocation entry with details")
        void shouldGetRevocationEntryWithDetails() throws IOException {
            // Given
            String json = """
                {
                  "version": 1,
                  "updated": "2026-02-01T00:00:00Z",
                  "revoked": [
                    {
                      "fingerprint": "sha256:abc123def456",
                      "reason": "Key compromise",
                      "revokedAt": "2026-01-15T00:00:00Z"
                    }
                  ]
                }
                """;
            Files.writeString(revocationListPath, json);
            CertificateRevocationList crl = CertificateRevocationList.load(revocationListPath);

            // When
            Optional<RevokedCertificateEntry> entry = crl.getRevocationEntry("sha256:abc123def456");

            // Then
            assertThat(entry).isPresent();
            assertThat(entry.get().fingerprint()).isEqualTo("sha256:abc123def456");
            assertThat(entry.get().reason()).isEqualTo("Key compromise");
            assertThat(entry.get().revokedAt()).isEqualTo(Instant.parse("2026-01-15T00:00:00Z"));
        }

        @Test
        @DisplayName("should return empty optional for non-revoked certificate")
        void shouldReturnEmptyForNonRevoked() throws IOException {
            // Given
            String json = """
                {
                  "version": 1,
                  "updated": "2026-02-01T00:00:00Z",
                  "revoked": []
                }
                """;
            Files.writeString(revocationListPath, json);
            CertificateRevocationList crl = CertificateRevocationList.load(revocationListPath);

            // When
            Optional<RevokedCertificateEntry> entry = crl.getRevocationEntry("sha256:unknown");

            // Then
            assertThat(entry).isEmpty();
        }
    }

    @Nested
    @DisplayName("Fingerprint computation")
    class FingerprintTests {

        @Test
        @DisplayName("should compute SHA-256 fingerprint from certificate bytes")
        void shouldComputeSha256Fingerprint() {
            // Given - a sample certificate content (simulated)
            byte[] certBytes = "test certificate content".getBytes();

            // When
            String fingerprint = CertificateRevocationList.computeFingerprint(certBytes);

            // Then
            assertThat(fingerprint).startsWith("sha256:");
            // SHA-256 produces 64 hex characters
            assertThat(fingerprint.substring(7)).hasSize(64);
        }

        @Test
        @DisplayName("should produce consistent fingerprint for same input")
        void shouldProduceConsistentFingerprint() {
            // Given
            byte[] certBytes = "test certificate content".getBytes();

            // When
            String fingerprint1 = CertificateRevocationList.computeFingerprint(certBytes);
            String fingerprint2 = CertificateRevocationList.computeFingerprint(certBytes);

            // Then
            assertThat(fingerprint1).isEqualTo(fingerprint2);
        }

        @Test
        @DisplayName("should produce different fingerprints for different inputs")
        void shouldProduceDifferentFingerprints() {
            // Given
            byte[] cert1 = "certificate 1".getBytes();
            byte[] cert2 = "certificate 2".getBytes();

            // When
            String fingerprint1 = CertificateRevocationList.computeFingerprint(cert1);
            String fingerprint2 = CertificateRevocationList.computeFingerprint(cert2);

            // Then
            assertThat(fingerprint1).isNotEqualTo(fingerprint2);
        }

        @Test
        @DisplayName("should throw on null input")
        void shouldThrowOnNullInput() {
            assertThatThrownBy(() -> CertificateRevocationList.computeFingerprint(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Creating empty list")
    class EmptyListTests {

        @Test
        @DisplayName("should create empty revocation list")
        void shouldCreateEmptyList() {
            // When
            CertificateRevocationList crl = CertificateRevocationList.empty();

            // Then
            assertThat(crl.getRevokedEntries()).isEmpty();
            assertThat(crl.getVersion()).isEqualTo(0);
            assertThat(crl.isRevoked("sha256:anything")).isFalse();
        }
    }

    @Nested
    @DisplayName("Saving to file")
    class SavingTests {

        @Test
        @DisplayName("should save revocation list to JSON file")
        void shouldSaveToJsonFile() throws IOException {
            // Given
            CertificateRevocationList crl = CertificateRevocationList.builder()
                .version(1)
                .updated(Instant.parse("2026-02-01T00:00:00Z"))
                .addRevoked(new RevokedCertificateEntry(
                    "sha256:abc123",
                    "Key compromise",
                    Instant.parse("2026-01-15T00:00:00Z")
                ))
                .build();

            // When
            crl.save(revocationListPath);

            // Then
            assertThat(Files.exists(revocationListPath)).isTrue();
            String content = Files.readString(revocationListPath);
            assertThat(content).contains("\"version\"");
            assertThat(content).contains("sha256:abc123");
            assertThat(content).contains("Key compromise");
        }

        @Test
        @DisplayName("should round-trip save and load")
        void shouldRoundTripSaveAndLoad() throws IOException {
            // Given
            CertificateRevocationList original = CertificateRevocationList.builder()
                .version(2)
                .updated(Instant.parse("2026-02-01T12:00:00Z"))
                .addRevoked(new RevokedCertificateEntry(
                    "sha256:fingerprint1",
                    "Reason 1",
                    Instant.parse("2026-01-10T00:00:00Z")
                ))
                .addRevoked(new RevokedCertificateEntry(
                    "sha256:fingerprint2",
                    "Reason 2",
                    Instant.parse("2026-01-20T00:00:00Z")
                ))
                .build();

            // When
            original.save(revocationListPath);
            CertificateRevocationList loaded = CertificateRevocationList.load(revocationListPath);

            // Then
            assertThat(loaded.getVersion()).isEqualTo(original.getVersion());
            assertThat(loaded.getUpdated()).isEqualTo(original.getUpdated());
            assertThat(loaded.getRevokedEntries()).hasSize(2);
            assertThat(loaded.isRevoked("sha256:fingerprint1")).isTrue();
            assertThat(loaded.isRevoked("sha256:fingerprint2")).isTrue();
        }
    }
}
