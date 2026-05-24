package uk.selfemploy.core.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.legal.SubmissionConfirmation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * JSONL-backed {@link DeclarationAuditLog} writing one canonical line per
 * confirmed Self Assessment final declaration submission.
 *
 * <p>The file is opened in
 * {@code APPEND + CREATE} mode for every write — no in-process buffer holds
 * unflushed lines, and the file is never re-read or rewritten. This makes the
 * log resilient to crashes mid-submission and makes after-the-fact tampering
 * detectable (line N's hash should match what was submitted at time N).
 *
 * <p>Line schema (canonical JSON, sorted keys for deterministic hashing):
 * <pre>
 * {
 *   "calculationId": "...",
 *   "confirmedAt":   "2026-01-15T09:30:00Z",
 *   "ninoHashSha256":"&lt;hex&gt;",
 *   "submissionHashSha256":"&lt;hex&gt;",
 *   "taxYear":       "2024-25",
 *   "userId":        "..."
 * }
 * </pre>
 *
 * <p>The {@code submissionHashSha256} is computed over the canonical JSON of
 * {@code {nino, taxYear, calculationId}} (with plaintext NINO) — this proves
 * what was submitted without requiring the raw HMRC payload to be persisted.
 * The plaintext NINO never reaches the file system; only its hash does.
 *
 * <p>File permissions: 0600 on POSIX systems (best-effort elsewhere). The
 * containing directory is created with 0700 where supported.
 */
public class FileSystemDeclarationAuditLog implements DeclarationAuditLog {

    private static final Logger log = LoggerFactory.getLogger(FileSystemDeclarationAuditLog.class);

    private static final Set<PosixFilePermission> OWNER_READ_WRITE_ONLY =
            PosixFilePermissions.fromString("rw-------");
    private static final Set<PosixFilePermission> OWNER_RWX_ONLY =
            PosixFilePermissions.fromString("rwx------");

    private final Path logFile;
    private final NinoHasher ninoHasher;
    private final ObjectMapper objectMapper;

    public FileSystemDeclarationAuditLog(Path logFile, NinoHasher ninoHasher) {
        this.logFile = logFile;
        this.ninoHasher = ninoHasher;
        // Sorted-keys ObjectMapper for deterministic submission hashing.
        this.objectMapper = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Override
    public void recordConfirmedSubmission(
            SubmissionConfirmation confirmation,
            String plaintextNino,
            String taxYearLabel,
            String calculationId
    ) throws IOException {
        if (confirmation == null) {
            throw new IllegalArgumentException("confirmation must not be null");
        }
        if (plaintextNino == null || plaintextNino.isBlank()) {
            throw new IllegalArgumentException("plaintextNino must not be null or blank");
        }
        if (taxYearLabel == null || taxYearLabel.isBlank()) {
            throw new IllegalArgumentException("taxYearLabel must not be null or blank");
        }
        if (calculationId == null || calculationId.isBlank()) {
            throw new IllegalArgumentException("calculationId must not be null or blank");
        }

        ensureLogDirectoryExists();

        String ninoHash = ninoHasher.hash(plaintextNino);
        String submissionHash = computeSubmissionHash(plaintextNino, taxYearLabel, calculationId);

        ObjectNode node = objectMapper.createObjectNode();
        node.put("calculationId", calculationId);
        node.put("confirmedAt", confirmation.confirmedAt().toString()); // ISO-8601 UTC
        node.put("ninoHashSha256", ninoHash);
        node.put("submissionHashSha256", submissionHash);
        node.put("taxYear", taxYearLabel);
        node.put("userId", confirmation.userId());

        String line = objectMapper.writeValueAsString(node) + System.lineSeparator();

        boolean newFile = !Files.exists(logFile);
        Files.writeString(
                logFile,
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
        if (newFile) {
            try {
                Files.setPosixFilePermissions(logFile, OWNER_READ_WRITE_ONLY);
            } catch (UnsupportedOperationException ignored) {
                log.debug("POSIX permissions unsupported; audit log at {} relies on default ACLs", logFile);
            }
        }
    }

    private void ensureLogDirectoryExists() throws IOException {
        Path parent = logFile.getParent();
        if (parent == null) {
            return;
        }
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
            try {
                Files.setPosixFilePermissions(parent, OWNER_RWX_ONLY);
            } catch (UnsupportedOperationException ignored) {
                log.debug("POSIX permissions unsupported on this filesystem for {}", parent);
            }
        }
    }

    private String computeSubmissionHash(String nino, String taxYear, String calculationId) {
        try {
            // Canonical JSON, sorted keys, plaintext NINO included locally only for hashing.
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("calculationId", calculationId);
            payload.put("nino", nino);
            payload.put("taxYear", taxYear);
            byte[] canonical = objectMapper.writeValueAsBytes(payload);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(canonical));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable on this JRE", e);
        } catch (IOException e) {
            throw new IllegalStateException("Canonical JSON serialisation failed for submission hash", e);
        }
    }
}
