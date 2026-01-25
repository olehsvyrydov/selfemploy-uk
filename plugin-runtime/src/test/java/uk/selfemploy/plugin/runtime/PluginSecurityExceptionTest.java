package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PluginSecurityException}.
 */
@DisplayName("PluginSecurityException")
class PluginSecurityExceptionTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Creates exception with plugin ID and message")
        void createsWithPluginIdAndMessage() {
            PluginSecurityException exception = new PluginSecurityException(
                "test-plugin",
                "Signature verification failed"
            );

            assertThat(exception.getMessage())
                .contains("test-plugin")
                .contains("Signature verification failed");
            assertThat(exception.getPluginId()).isEqualTo("test-plugin");
        }

        @Test
        @DisplayName("Creates exception with plugin ID, message, and cause")
        void createsWithPluginIdMessageAndCause() {
            Throwable cause = new SecurityException("Invalid signature");
            PluginSecurityException exception = new PluginSecurityException(
                "test-plugin",
                "Signature verification failed",
                cause
            );

            assertThat(exception.getMessage())
                .contains("test-plugin")
                .contains("Signature verification failed");
            assertThat(exception.getPluginId()).isEqualTo("test-plugin");
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Creates exception with security violation type")
        void createsWithSecurityViolationType() {
            PluginSecurityException exception = new PluginSecurityException(
                "test-plugin",
                PluginSecurityException.SecurityViolationType.INVALID_SIGNATURE,
                "JAR signature is invalid"
            );

            assertThat(exception.getPluginId()).isEqualTo("test-plugin");
            assertThat(exception.getViolationType())
                .isEqualTo(PluginSecurityException.SecurityViolationType.INVALID_SIGNATURE);
            assertThat(exception.getMessage())
                .contains("INVALID_SIGNATURE")
                .contains("JAR signature is invalid");
        }
    }

    @Nested
    @DisplayName("Security violation types")
    class SecurityViolationTypes {

        @Test
        @DisplayName("INVALID_SIGNATURE indicates untrusted JAR")
        void invalidSignatureType() {
            PluginSecurityException exception = new PluginSecurityException(
                "plugin",
                PluginSecurityException.SecurityViolationType.INVALID_SIGNATURE,
                "detail"
            );
            assertThat(exception.getViolationType())
                .isEqualTo(PluginSecurityException.SecurityViolationType.INVALID_SIGNATURE);
        }

        @Test
        @DisplayName("UNSIGNED_PLUGIN indicates missing signature")
        void unsignedPluginType() {
            PluginSecurityException exception = new PluginSecurityException(
                "plugin",
                PluginSecurityException.SecurityViolationType.UNSIGNED_PLUGIN,
                "detail"
            );
            assertThat(exception.getViolationType())
                .isEqualTo(PluginSecurityException.SecurityViolationType.UNSIGNED_PLUGIN);
        }

        @Test
        @DisplayName("UNTRUSTED_PUBLISHER indicates unknown signer")
        void untrustedPublisherType() {
            PluginSecurityException exception = new PluginSecurityException(
                "plugin",
                PluginSecurityException.SecurityViolationType.UNTRUSTED_PUBLISHER,
                "detail"
            );
            assertThat(exception.getViolationType())
                .isEqualTo(PluginSecurityException.SecurityViolationType.UNTRUSTED_PUBLISHER);
        }

        @Test
        @DisplayName("PERMISSION_DENIED indicates missing permission")
        void permissionDeniedType() {
            PluginSecurityException exception = new PluginSecurityException(
                "plugin",
                PluginSecurityException.SecurityViolationType.PERMISSION_DENIED,
                "detail"
            );
            assertThat(exception.getViolationType())
                .isEqualTo(PluginSecurityException.SecurityViolationType.PERMISSION_DENIED);
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class Inheritance {

        @Test
        @DisplayName("Extends PluginException")
        void extendsPluginException() {
            PluginSecurityException exception = new PluginSecurityException("plugin", "message");
            assertThat(exception).isInstanceOf(PluginException.class);
        }

        @Test
        @DisplayName("Is a RuntimeException")
        void isRuntimeException() {
            PluginSecurityException exception = new PluginSecurityException("plugin", "message");
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }
}
