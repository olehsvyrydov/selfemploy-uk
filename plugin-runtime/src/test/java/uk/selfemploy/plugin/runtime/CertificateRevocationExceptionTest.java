package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CertificateRevocationException}.
 */
@DisplayName("CertificateRevocationException")
class CertificateRevocationExceptionTest {

    @Nested
    @DisplayName("Parsing error constructor")
    class ParsingErrorTests {

        @Test
        @DisplayName("should create exception with message only")
        void shouldCreateExceptionWithMessageOnly() {
            // When
            CertificateRevocationException ex = new CertificateRevocationException("Parse error");

            // Then
            assertThat(ex.getMessage()).isEqualTo("Parse error");
            assertThat(ex.getFingerprint()).isNull();
            assertThat(ex.getReason()).isNull();
            assertThat(ex.isRevocationError()).isFalse();
        }

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            // Given
            RuntimeException cause = new RuntimeException("Root cause");

            // When
            CertificateRevocationException ex = new CertificateRevocationException("Parse error", cause);

            // Then
            assertThat(ex.getMessage()).isEqualTo("Parse error");
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getFingerprint()).isNull();
            assertThat(ex.getReason()).isNull();
            assertThat(ex.isRevocationError()).isFalse();
        }
    }

    @Nested
    @DisplayName("Revocation error constructor")
    class RevocationErrorTests {

        @Test
        @DisplayName("should create exception for revoked certificate")
        void shouldCreateExceptionForRevokedCertificate() {
            // When
            CertificateRevocationException ex = new CertificateRevocationException(
                "sha256:abc123",
                "Key compromise"
            );

            // Then
            assertThat(ex.getFingerprint()).isEqualTo("sha256:abc123");
            assertThat(ex.getReason()).isEqualTo("Key compromise");
            assertThat(ex.isRevocationError()).isTrue();
            assertThat(ex.getMessage()).contains("sha256:abc123");
            assertThat(ex.getMessage()).contains("Key compromise");
        }

        @Test
        @DisplayName("should handle null reason")
        void shouldHandleNullReason() {
            // When
            CertificateRevocationException ex = new CertificateRevocationException(
                "sha256:abc123",
                (String) null  // Cast to disambiguate from (Throwable) constructor
            );

            // Then
            assertThat(ex.getReason()).isNull();
            assertThat(ex.getMessage()).contains("Unknown");
            assertThat(ex.isRevocationError()).isTrue();
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class InheritanceTests {

        @Test
        @DisplayName("should extend PluginException")
        void shouldExtendPluginException() {
            // When
            CertificateRevocationException ex = new CertificateRevocationException("error");

            // Then
            assertThat(ex).isInstanceOf(PluginException.class);
        }
    }
}
