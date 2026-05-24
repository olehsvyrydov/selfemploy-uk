package uk.selfemploy.core.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Set;

/**
 * Salted SHA-256 hasher for NINOs intended for the local declaration audit log.
 *
 * <p>The declaration audit log must record
 * that <em>a</em> NINO was submitted without storing the plaintext value
 * (GDPR Art.5(1)(c) data minimisation). Plain SHA-256 of a NINO is trivially
 * brute-forceable — the space is ~100 million values — so a per-install random
 * salt is used. The salt is stored alongside the audit directory in a 0600 file;
 * this is the same threat model as the random-key-seed approach
 * (see {@code EncryptedFileTokenStorage}): an attacker with local read access
 * has already won.
 *
 * <p>Format: {@code SHA-256(salt || nino)} hex-encoded, lower-case.
 *
 * <p>This class is not for password hashing — for that, use PBKDF2/Argon2.
 * It exists purely to make local-only audit lines non-reversible.
 */
public class NinoHasher {

    private static final Logger log = LoggerFactory.getLogger(NinoHasher.class);

    private static final int SALT_LENGTH = 16;
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final Set<PosixFilePermission> OWNER_READ_WRITE_ONLY =
            PosixFilePermissions.fromString("rw-------");

    private final Path saltPath;
    private final byte[] salt;

    /**
     * Constructs a hasher backed by a per-install salt file. If the file does
     * not exist, a fresh 16-byte salt is generated and persisted with 0600
     * permissions (best-effort on Windows).
     *
     * @param saltPath path to the per-install salt file
     * @throws IOException if the salt file cannot be read or created
     */
    public NinoHasher(Path saltPath) throws IOException {
        this.saltPath = saltPath;
        this.salt = loadOrCreateSalt(saltPath);
    }

    /**
     * Hashes a NINO with the per-install salt.
     *
     * @param nino plaintext National Insurance Number (any format accepted by HMRC)
     * @return lower-case hex SHA-256 of {@code salt || nino}
     * @throws IllegalArgumentException if {@code nino} is null or blank
     */
    public String hash(String nino) {
        if (nino == null || nino.isBlank()) {
            throw new IllegalArgumentException("nino must not be null or blank");
        }
        try {
            MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            md.update(salt);
            byte[] digest = md.digest(nino.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JRE.
            throw new IllegalStateException("SHA-256 unavailable on this JRE", e);
        }
    }

    private static byte[] loadOrCreateSalt(Path saltPath) throws IOException {
        if (Files.exists(saltPath)) {
            byte[] existing = Files.readAllBytes(saltPath);
            if (existing.length != SALT_LENGTH) {
                throw new IOException(
                        "Salt file has unexpected length " + existing.length + " at " + saltPath);
            }
            return existing;
        }
        Files.createDirectories(saltPath.getParent());
        byte[] fresh = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(fresh);
        Files.write(saltPath, fresh);
        try {
            Files.setPosixFilePermissions(saltPath, OWNER_READ_WRITE_ONLY);
        } catch (UnsupportedOperationException ignored) {
            log.debug("POSIX permissions unsupported on this filesystem; salt file at {} relies on default ACLs", saltPath);
        }
        return fresh;
    }
}
