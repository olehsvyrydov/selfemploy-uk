package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PluginSignatureVerifier}.
 */
@DisplayName("PluginSignatureVerifier")
class PluginSignatureVerifierTest {

    private PluginSignatureVerifier verifier;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        verifier = new PluginSignatureVerifier();
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Creates verifier with default trust store")
        void createsWithDefaultTrustStore() {
            PluginSignatureVerifier defaultVerifier = new PluginSignatureVerifier();
            assertThat(defaultVerifier).isNotNull();
            assertThat(defaultVerifier.getTrustedPublishers()).isEmpty();
        }

        @Test
        @DisplayName("Creates verifier with custom trusted publishers")
        void createsWithCustomTrustedPublishers() {
            Set<String> publishers = Set.of("CN=Trusted Publisher, O=Example Corp");
            PluginSignatureVerifier customVerifier = new PluginSignatureVerifier(publishers);

            assertThat(customVerifier.getTrustedPublishers())
                .containsExactlyInAnyOrderElementsOf(publishers);
        }
    }

    @Nested
    @DisplayName("Signature verification")
    class SignatureVerification {

        @Test
        @DisplayName("Unsigned JAR fails verification when signature required")
        void unsignedJarFailsVerification() throws Exception {
            Path unsignedJar = createUnsignedJar("test-plugin.jar");

            PluginSignatureVerifier strictVerifier = PluginSignatureVerifier.builder()
                .requireSignature(true)
                .build();

            PluginSignatureVerifier.VerificationResult result =
                strictVerifier.verify(unsignedJar);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getStatus())
                .isEqualTo(PluginSignatureVerifier.SignatureStatus.UNSIGNED);
        }

        @Test
        @DisplayName("Unsigned JAR passes when signature not required")
        void unsignedJarPassesWhenNotRequired() throws Exception {
            Path unsignedJar = createUnsignedJar("optional-plugin.jar");

            PluginSignatureVerifier lenientVerifier = PluginSignatureVerifier.builder()
                .requireSignature(false)
                .build();

            PluginSignatureVerifier.VerificationResult result =
                lenientVerifier.verify(unsignedJar);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getStatus())
                .isEqualTo(PluginSignatureVerifier.SignatureStatus.UNSIGNED);
        }

        @Test
        @DisplayName("Non-existent JAR throws exception")
        void nonExistentJarThrowsException() {
            Path nonExistent = tempDir.resolve("non-existent.jar");

            assertThatThrownBy(() -> verifier.verify(nonExistent))
                .isInstanceOf(PluginSecurityException.class)
                .hasMessageContaining("non-existent.jar");
        }

        @Test
        @DisplayName("Invalid JAR file throws exception")
        void invalidJarThrowsException() throws Exception {
            Path invalidJar = tempDir.resolve("invalid.jar");
            Files.writeString(invalidJar, "This is not a JAR file");

            assertThatThrownBy(() -> verifier.verify(invalidJar))
                .isInstanceOf(PluginSecurityException.class);
        }
    }

    @Nested
    @DisplayName("Trust verification")
    class TrustVerification {

        @Test
        @DisplayName("Adding trusted publisher")
        void addingTrustedPublisher() {
            String publisherDN = "CN=My Company, O=My Org, C=UK";

            verifier.addTrustedPublisher(publisherDN);

            assertThat(verifier.getTrustedPublishers()).contains(publisherDN);
        }

        @Test
        @DisplayName("Removing trusted publisher")
        void removingTrustedPublisher() {
            String publisherDN = "CN=My Company, O=My Org, C=UK";
            verifier.addTrustedPublisher(publisherDN);

            verifier.removeTrustedPublisher(publisherDN);

            assertThat(verifier.getTrustedPublishers()).doesNotContain(publisherDN);
        }

        @Test
        @DisplayName("Clearing all trusted publishers")
        void clearingTrustedPublishers() {
            verifier.addTrustedPublisher("CN=Publisher1");
            verifier.addTrustedPublisher("CN=Publisher2");

            verifier.clearTrustedPublishers();

            assertThat(verifier.getTrustedPublishers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Verification result")
    class VerificationResultTests {

        @Test
        @DisplayName("Success result contains certificate info")
        void successResultContainsCertInfo() {
            PluginSignatureVerifier.VerificationResult result =
                PluginSignatureVerifier.VerificationResult.trusted(
                    "CN=Trusted Publisher",
                    List.of()
                );

            assertThat(result.isValid()).isTrue();
            assertThat(result.isTrusted()).isTrue();
            assertThat(result.getSignerDN()).isEqualTo("CN=Trusted Publisher");
            assertThat(result.getStatus())
                .isEqualTo(PluginSignatureVerifier.SignatureStatus.TRUSTED);
        }

        @Test
        @DisplayName("Untrusted result indicates unknown publisher")
        void untrustedResultIndicatesUnknownPublisher() {
            PluginSignatureVerifier.VerificationResult result =
                PluginSignatureVerifier.VerificationResult.untrusted(
                    "CN=Unknown Publisher",
                    List.of()
                );

            assertThat(result.isValid()).isTrue();
            assertThat(result.isTrusted()).isFalse();
            assertThat(result.getSignerDN()).isEqualTo("CN=Unknown Publisher");
            assertThat(result.getStatus())
                .isEqualTo(PluginSignatureVerifier.SignatureStatus.UNTRUSTED);
        }

        @Test
        @DisplayName("Invalid result indicates corrupted signature")
        void invalidResultIndicatesCorruption() {
            PluginSignatureVerifier.VerificationResult result =
                PluginSignatureVerifier.VerificationResult.invalid(
                    "Signature does not match JAR contents"
                );

            assertThat(result.isValid()).isFalse();
            assertThat(result.isTrusted()).isFalse();
            assertThat(result.getErrorMessage())
                .contains("Signature does not match");
            assertThat(result.getStatus())
                .isEqualTo(PluginSignatureVerifier.SignatureStatus.INVALID);
        }

        @Test
        @DisplayName("Unsigned result when signature required")
        void unsignedResultWhenRequired() {
            PluginSignatureVerifier.VerificationResult result =
                PluginSignatureVerifier.VerificationResult.unsigned();

            assertThat(result.isValid()).isFalse();
            assertThat(result.isTrusted()).isFalse();
            assertThat(result.getSignerDN()).isNull();
            assertThat(result.getStatus())
                .isEqualTo(PluginSignatureVerifier.SignatureStatus.UNSIGNED);
        }

        @Test
        @DisplayName("Unsigned result when signature optional")
        void unsignedResultWhenOptional() {
            PluginSignatureVerifier.VerificationResult result =
                PluginSignatureVerifier.VerificationResult.unsignedValid();

            assertThat(result.isValid()).isTrue();
            assertThat(result.isTrusted()).isFalse();
            assertThat(result.getSignerDN()).isNull();
            assertThat(result.getStatus())
                .isEqualTo(PluginSignatureVerifier.SignatureStatus.UNSIGNED);
        }
    }

    @Nested
    @DisplayName("Builder pattern")
    class BuilderPattern {

        @Test
        @DisplayName("Builder creates verifier with all options")
        void builderCreatesVerifier() {
            PluginSignatureVerifier customVerifier = PluginSignatureVerifier.builder()
                .requireSignature(true)
                .addTrustedPublisher("CN=Publisher1")
                .addTrustedPublisher("CN=Publisher2")
                .build();

            assertThat(customVerifier.isSignatureRequired()).isTrue();
            assertThat(customVerifier.getTrustedPublishers())
                .containsExactlyInAnyOrder("CN=Publisher1", "CN=Publisher2");
        }

        @Test
        @DisplayName("Builder with trusted publishers collection")
        void builderWithPublishersCollection() {
            Set<String> publishers = Set.of("CN=P1", "CN=P2", "CN=P3");

            PluginSignatureVerifier customVerifier = PluginSignatureVerifier.builder()
                .trustedPublishers(publishers)
                .build();

            assertThat(customVerifier.getTrustedPublishers())
                .containsExactlyInAnyOrderElementsOf(publishers);
        }
    }

    // Helper methods

    private Path createUnsignedJar(String name) throws Exception {
        Path jarPath = tempDir.resolve(name);

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            // Add a simple entry
            JarEntry entry = new JarEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(entry);
            jos.write("Manifest-Version: 1.0\n".getBytes());
            jos.closeEntry();

            // Add a dummy class entry
            JarEntry classEntry = new JarEntry("com/example/TestClass.class");
            jos.putNextEntry(classEntry);
            jos.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}); // class magic
            jos.closeEntry();
        }

        return jarPath;
    }
}
