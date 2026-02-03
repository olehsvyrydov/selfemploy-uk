package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RevokedCertificateEntry}.
 */
@DisplayName("RevokedCertificateEntry")
class RevokedCertificateEntryTest {

    private static final Instant REVOKED_AT = Instant.parse("2026-01-15T00:00:00Z");

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("should create entry with valid parameters")
        void shouldCreateEntryWithValidParameters() {
            // When
            RevokedCertificateEntry entry = new RevokedCertificateEntry(
                "sha256:abc123def456",
                "Key compromise",
                REVOKED_AT
            );

            // Then
            assertThat(entry.fingerprint()).isEqualTo("sha256:abc123def456");
            assertThat(entry.reason()).isEqualTo("Key compromise");
            assertThat(entry.revokedAt()).isEqualTo(REVOKED_AT);
        }

        @Test
        @DisplayName("should normalize fingerprint to lowercase")
        void shouldNormalizeFingerprintToLowercase() {
            // When
            RevokedCertificateEntry entry = new RevokedCertificateEntry(
                "SHA256:ABC123DEF456",
                "Key compromise",
                REVOKED_AT
            );

            // Then
            assertThat(entry.fingerprint()).isEqualTo("sha256:abc123def456");
        }

        @Test
        @DisplayName("should throw on null fingerprint")
        void shouldThrowOnNullFingerprint() {
            assertThatThrownBy(() -> new RevokedCertificateEntry(null, "reason", REVOKED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fingerprint");
        }

        @Test
        @DisplayName("should throw on null revokedAt")
        void shouldThrowOnNullRevokedAt() {
            assertThatThrownBy(() -> new RevokedCertificateEntry("sha256:abc", "reason", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("revokedAt");
        }

        @Test
        @DisplayName("should allow null reason")
        void shouldAllowNullReason() {
            // When
            RevokedCertificateEntry entry = new RevokedCertificateEntry(
                "sha256:abc123",
                null,
                REVOKED_AT
            );

            // Then
            assertThat(entry.reason()).isNull();
        }

        @Test
        @DisplayName("should throw when fingerprint does not start with sha256:")
        void shouldThrowWhenFingerprintInvalid() {
            assertThatThrownBy(() -> new RevokedCertificateEntry("md5:abc123", "reason", REVOKED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sha256:");
        }
    }

    @Nested
    @DisplayName("Matching")
    class MatchingTests {

        @Test
        @DisplayName("should match exact fingerprint")
        void shouldMatchExactFingerprint() {
            // Given
            RevokedCertificateEntry entry = new RevokedCertificateEntry(
                "sha256:abc123",
                "reason",
                REVOKED_AT
            );

            // When/Then
            assertThat(entry.matches("sha256:abc123")).isTrue();
        }

        @Test
        @DisplayName("should match case-insensitively")
        void shouldMatchCaseInsensitively() {
            // Given
            RevokedCertificateEntry entry = new RevokedCertificateEntry(
                "sha256:abc123",
                "reason",
                REVOKED_AT
            );

            // When/Then
            assertThat(entry.matches("SHA256:ABC123")).isTrue();
            assertThat(entry.matches("Sha256:Abc123")).isTrue();
        }

        @Test
        @DisplayName("should not match different fingerprint")
        void shouldNotMatchDifferentFingerprint() {
            // Given
            RevokedCertificateEntry entry = new RevokedCertificateEntry(
                "sha256:abc123",
                "reason",
                REVOKED_AT
            );

            // When/Then
            assertThat(entry.matches("sha256:xyz789")).isFalse();
        }

        @Test
        @DisplayName("should return false for null fingerprint")
        void shouldReturnFalseForNullFingerprint() {
            // Given
            RevokedCertificateEntry entry = new RevokedCertificateEntry(
                "sha256:abc123",
                "reason",
                REVOKED_AT
            );

            // When/Then
            assertThat(entry.matches(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("should include all fields in toString")
        void shouldIncludeAllFieldsInToString() {
            // Given
            RevokedCertificateEntry entry = new RevokedCertificateEntry(
                "sha256:abc123",
                "Key compromise",
                REVOKED_AT
            );

            // When
            String result = entry.toString();

            // Then
            assertThat(result).contains("sha256:abc123");
            assertThat(result).contains("Key compromise");
            assertThat(result).contains(REVOKED_AT.toString());
        }
    }
}
