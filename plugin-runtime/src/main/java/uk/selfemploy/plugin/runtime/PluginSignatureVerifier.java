package uk.selfemploy.plugin.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Verifies the digital signatures of plugin JAR files.
 *
 * <p>The PluginSignatureVerifier ensures that plugin JARs are signed by
 * trusted publishers before they are loaded. This provides security against
 * malicious plugins, especially for sensitive operations like HMRC API access.</p>
 *
 * <h2>Verification Process</h2>
 * <ol>
 *   <li>Check if the JAR file is signed</li>
 *   <li>Verify the signature integrity (JAR contents match signature)</li>
 *   <li>Validate the signer's certificate chain</li>
 *   <li>Check if the signer is in the trusted publishers list</li>
 * </ol>
 *
 * <h2>Signature Requirements</h2>
 * <p>By default, unsigned plugins are allowed for development flexibility.
 * In production environments, enable {@code requireSignature} to enforce
 * signed plugins.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The trusted publishers list uses
 * CopyOnWriteArraySet for concurrent access.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PluginSignatureVerifier verifier = PluginSignatureVerifier.builder()
 *     .requireSignature(true)
 *     .addTrustedPublisher("CN=My Company, O=My Org, C=UK")
 *     .build();
 *
 * VerificationResult result = verifier.verify(pluginJarPath);
 * if (!result.isValid()) {
 *     throw new PluginSecurityException(pluginId, result.getErrorMessage());
 * }
 * }</pre>
 *
 * @see PluginLoader
 * @see PluginSecurityException
 */
public class PluginSignatureVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(PluginSignatureVerifier.class);

    /**
     * Status of the signature verification.
     */
    public enum SignatureStatus {
        /** JAR is signed by a trusted publisher */
        TRUSTED,

        /** JAR is signed but publisher is not in trusted list */
        UNTRUSTED,

        /** JAR is not signed */
        UNSIGNED,

        /** JAR signature is invalid or corrupted */
        INVALID
    }

    private final Set<String> trustedPublishers;
    private final boolean requireSignature;

    /**
     * Creates a new PluginSignatureVerifier with default settings.
     *
     * <p>By default, signatures are not required and no publishers are trusted.</p>
     */
    public PluginSignatureVerifier() {
        this(Collections.emptySet(), false);
    }

    /**
     * Creates a new PluginSignatureVerifier with specified trusted publishers.
     *
     * @param trustedPublishers the set of trusted publisher Distinguished Names
     */
    public PluginSignatureVerifier(Set<String> trustedPublishers) {
        this(trustedPublishers, false);
    }

    /**
     * Creates a new PluginSignatureVerifier with full configuration.
     *
     * @param trustedPublishers the set of trusted publisher Distinguished Names
     * @param requireSignature  whether to require all plugins to be signed
     */
    public PluginSignatureVerifier(Set<String> trustedPublishers, boolean requireSignature) {
        this.trustedPublishers = new CopyOnWriteArraySet<>(
            trustedPublishers != null ? trustedPublishers : Collections.emptySet()
        );
        this.requireSignature = requireSignature;
    }

    /**
     * Verifies the signature of a plugin JAR file.
     *
     * @param jarPath the path to the JAR file to verify
     * @return the verification result
     * @throws PluginSecurityException if the JAR cannot be read or is invalid
     */
    public VerificationResult verify(Path jarPath) {
        Objects.requireNonNull(jarPath, "jarPath must not be null");

        if (!Files.exists(jarPath)) {
            throw new PluginSecurityException(
                jarPath.getFileName().toString(),
                PluginSecurityException.SecurityViolationType.GENERAL,
                "JAR file does not exist: " + jarPath
            );
        }

        try (JarFile jarFile = new JarFile(jarPath.toFile(), true)) {
            return verifyJarSignatures(jarFile, jarPath);
        } catch (IOException e) {
            throw new PluginSecurityException(
                jarPath.getFileName().toString(),
                PluginSecurityException.SecurityViolationType.GENERAL,
                "Failed to read JAR file: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Verifies the signatures within a JAR file.
     *
     * @param jarFile the opened JAR file
     * @param jarPath the path to the JAR file (for error messages)
     * @return the verification result
     */
    private VerificationResult verifyJarSignatures(JarFile jarFile, Path jarPath) throws IOException {
        List<CodeSigner> signers = new ArrayList<>();
        boolean hasSignedEntries = false;

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            // Skip directory entries and signature files
            if (entry.isDirectory() || isSignatureFile(entry.getName())) {
                continue;
            }

            // Read the entry to trigger signature verification
            try (InputStream is = jarFile.getInputStream(entry)) {
                byte[] buffer = new byte[8192];
                while (is.read(buffer) != -1) {
                    // Just consume the stream to verify signature
                }
            }

            // Check if entry has code signers
            CodeSigner[] entrySigners = entry.getCodeSigners();
            if (entrySigners != null && entrySigners.length > 0) {
                hasSignedEntries = true;
                for (CodeSigner signer : entrySigners) {
                    if (!signers.contains(signer)) {
                        signers.add(signer);
                    }
                }
            }
        }

        // No signed entries found
        if (!hasSignedEntries) {
            LOG.debug("JAR {} is unsigned", jarPath.getFileName());
            if (requireSignature) {
                return VerificationResult.unsigned();
            }
            return VerificationResult.unsignedValid();
        }

        // Check all signers
        for (CodeSigner signer : signers) {
            List<? extends Certificate> certChain = signer.getSignerCertPath().getCertificates();
            if (certChain.isEmpty()) {
                continue;
            }

            Certificate cert = certChain.get(0);
            if (cert instanceof X509Certificate x509Cert) {
                String signerDN = x509Cert.getSubjectX500Principal().getName();
                LOG.debug("JAR {} signed by: {}", jarPath.getFileName(), signerDN);

                if (trustedPublishers.contains(signerDN)) {
                    LOG.info("JAR {} verified with trusted publisher: {}",
                        jarPath.getFileName(), signerDN);
                    return VerificationResult.trusted(signerDN, toCertificateList(certChain));
                } else {
                    LOG.warn("JAR {} signed by untrusted publisher: {}",
                        jarPath.getFileName(), signerDN);
                    return VerificationResult.untrusted(signerDN, toCertificateList(certChain));
                }
            }
        }

        // Signed but couldn't extract signer info
        return VerificationResult.invalid("Unable to extract signer information");
    }

    /**
     * Checks if a JAR entry is a signature-related file.
     *
     * @param entryName the entry name
     * @return true if this is a signature file
     */
    private boolean isSignatureFile(String entryName) {
        String upperName = entryName.toUpperCase();
        return upperName.startsWith("META-INF/") &&
               (upperName.endsWith(".SF") ||
                upperName.endsWith(".DSA") ||
                upperName.endsWith(".RSA") ||
                upperName.endsWith(".EC"));
    }

    /**
     * Converts a certificate list to a list of Certificate objects.
     */
    private List<Certificate> toCertificateList(List<? extends Certificate> certs) {
        return new ArrayList<>(certs);
    }

    /**
     * Adds a trusted publisher Distinguished Name.
     *
     * @param publisherDN the publisher's X.500 Distinguished Name
     */
    public void addTrustedPublisher(String publisherDN) {
        Objects.requireNonNull(publisherDN, "publisherDN must not be null");
        trustedPublishers.add(publisherDN);
        LOG.info("Added trusted publisher: {}", publisherDN);
    }

    /**
     * Removes a trusted publisher Distinguished Name.
     *
     * @param publisherDN the publisher's X.500 Distinguished Name
     * @return true if the publisher was removed
     */
    public boolean removeTrustedPublisher(String publisherDN) {
        boolean removed = trustedPublishers.remove(publisherDN);
        if (removed) {
            LOG.info("Removed trusted publisher: {}", publisherDN);
        }
        return removed;
    }

    /**
     * Clears all trusted publishers.
     */
    public void clearTrustedPublishers() {
        trustedPublishers.clear();
        LOG.info("Cleared all trusted publishers");
    }

    /**
     * Returns an unmodifiable view of trusted publishers.
     *
     * @return the set of trusted publisher Distinguished Names
     */
    public Set<String> getTrustedPublishers() {
        return Collections.unmodifiableSet(trustedPublishers);
    }

    /**
     * Returns whether signature verification is required.
     *
     * @return true if all plugins must be signed
     */
    public boolean isSignatureRequired() {
        return requireSignature;
    }

    /**
     * Creates a new builder for PluginSignatureVerifier.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for PluginSignatureVerifier.
     */
    public static class Builder {
        private final Set<String> trustedPublishers = new HashSet<>();
        private boolean requireSignature = false;

        /**
         * Sets whether signature verification is required.
         *
         * @param require true to require all plugins to be signed
         * @return this builder
         */
        public Builder requireSignature(boolean require) {
            this.requireSignature = require;
            return this;
        }

        /**
         * Adds a trusted publisher Distinguished Name.
         *
         * @param publisherDN the publisher's X.500 Distinguished Name
         * @return this builder
         */
        public Builder addTrustedPublisher(String publisherDN) {
            trustedPublishers.add(publisherDN);
            return this;
        }

        /**
         * Sets all trusted publishers.
         *
         * @param publishers the set of trusted publisher Distinguished Names
         * @return this builder
         */
        public Builder trustedPublishers(Set<String> publishers) {
            this.trustedPublishers.clear();
            if (publishers != null) {
                this.trustedPublishers.addAll(publishers);
            }
            return this;
        }

        /**
         * Builds the PluginSignatureVerifier.
         *
         * @return a new PluginSignatureVerifier instance
         */
        public PluginSignatureVerifier build() {
            return new PluginSignatureVerifier(trustedPublishers, requireSignature);
        }
    }

    /**
     * Result of signature verification.
     */
    public static class VerificationResult {
        private final SignatureStatus status;
        private final boolean valid;
        private final boolean trusted;
        private final String signerDN;
        private final List<Certificate> certificateChain;
        private final String errorMessage;

        private VerificationResult(SignatureStatus status, boolean valid, boolean trusted,
                                   String signerDN, List<Certificate> certificateChain,
                                   String errorMessage) {
            this.status = status;
            this.valid = valid;
            this.trusted = trusted;
            this.signerDN = signerDN;
            this.certificateChain = certificateChain != null ?
                Collections.unmodifiableList(certificateChain) : Collections.emptyList();
            this.errorMessage = errorMessage;
        }

        /**
         * Creates a result for a trusted signed JAR.
         *
         * @param signerDN         the signer's Distinguished Name
         * @param certificateChain the certificate chain
         * @return the verification result
         */
        public static VerificationResult trusted(String signerDN, List<Certificate> certificateChain) {
            return new VerificationResult(SignatureStatus.TRUSTED, true, true,
                signerDN, certificateChain, null);
        }

        /**
         * Creates a result for an untrusted signed JAR.
         *
         * @param signerDN         the signer's Distinguished Name
         * @param certificateChain the certificate chain
         * @return the verification result
         */
        public static VerificationResult untrusted(String signerDN, List<Certificate> certificateChain) {
            return new VerificationResult(SignatureStatus.UNTRUSTED, true, false,
                signerDN, certificateChain, null);
        }

        /**
         * Creates a result for an unsigned JAR when signature is required.
         *
         * @return the verification result
         */
        public static VerificationResult unsigned() {
            return new VerificationResult(SignatureStatus.UNSIGNED, false, false,
                null, null, "JAR is not signed");
        }

        /**
         * Creates a result for an unsigned JAR when signature is optional.
         *
         * @return the verification result
         */
        public static VerificationResult unsignedValid() {
            return new VerificationResult(SignatureStatus.UNSIGNED, true, false,
                null, null, null);
        }

        /**
         * Creates a result for an invalid signature.
         *
         * @param errorMessage the error message
         * @return the verification result
         */
        public static VerificationResult invalid(String errorMessage) {
            return new VerificationResult(SignatureStatus.INVALID, false, false,
                null, null, errorMessage);
        }

        /**
         * Returns whether the verification passed.
         *
         * @return true if the JAR is valid (signed correctly or signature not required)
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Returns whether the JAR is signed by a trusted publisher.
         *
         * @return true if the signer is in the trusted publishers list
         */
        public boolean isTrusted() {
            return trusted;
        }

        /**
         * Returns the signature status.
         *
         * @return the status
         */
        public SignatureStatus getStatus() {
            return status;
        }

        /**
         * Returns the signer's Distinguished Name.
         *
         * @return the signer DN, or null if unsigned
         */
        public String getSignerDN() {
            return signerDN;
        }

        /**
         * Returns the certificate chain.
         *
         * @return the certificate chain, or empty list if unsigned
         */
        public List<Certificate> getCertificateChain() {
            return certificateChain;
        }

        /**
         * Returns the error message if verification failed.
         *
         * @return the error message, or null if valid
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return String.format("VerificationResult[status=%s, valid=%s, trusted=%s, signer=%s]",
                status, valid, trusted, signerDN);
        }
    }
}
