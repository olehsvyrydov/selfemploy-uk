package uk.selfemploy.core.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.common.legal.SubmissionConfirmation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD tests for {@link FileSystemDeclarationAuditLog}.
 *
 * <p>Validates wire format, append-only behaviour, and the absolute requirement
 * that plaintext NINO never appears in the log file.
 */
@DisplayName("FileSystemDeclarationAuditLog")
class FileSystemDeclarationAuditLogTest {

    private static final String TEST_NINO = "AA123456A";
    private static final String TEST_TAX_YEAR_LABEL = "2024-25";
    private static final String TEST_CALC_ID = "calc-xyz-001";
    private static final SubmissionConfirmation CONFIRMATION =
        new SubmissionConfirmation("user-test", true, Instant.parse("2026-01-15T09:30:00Z"));

    @TempDir
    Path tempDir;

    private Path logFile;
    private FileSystemDeclarationAuditLog auditLog;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        logFile = tempDir.resolve("audit").resolve("declarations.jsonl");
        Path saltFile = tempDir.resolve("audit").resolve(".nino-salt");
        // Ensure parent exists so salt file can be created lazily.
        Files.createDirectories(saltFile.getParent());
        NinoHasher hasher = new NinoHasher(saltFile);
        auditLog = new FileSystemDeclarationAuditLog(logFile, hasher);
    }

    @Test
    @DisplayName("should append one JSONL line per confirmed submission")
    void shouldAppendOneLinePerSubmission() throws IOException {
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, TEST_CALC_ID);
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, "calc-002");

        List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(2);
        // Each line must be parseable JSON
        for (String line : lines) {
            assertThat(line).isNotBlank();
            JsonNode node = objectMapper.readTree(line);
            assertThat(node.isObject()).isTrue();
        }
    }

    @Test
    @DisplayName("should write ISO-8601 UTC timestamp, hashed NINO, and submission hash")
    void shouldWriteRequiredFields() throws IOException {
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, TEST_CALC_ID);

        JsonNode line = objectMapper.readTree(Files.readAllLines(logFile).get(0));

        // ISO-8601 / RFC 3339 UTC
        assertThat(line.get("confirmedAt").asText()).isEqualTo("2026-01-15T09:30:00Z");
        // hashed NINO present, hex, 64 chars
        String ninoHash = line.get("ninoHashSha256").asText();
        assertThat(ninoHash).matches("[0-9a-f]{64}");
        // submission hash present
        String subHash = line.get("submissionHashSha256").asText();
        assertThat(subHash).matches("[0-9a-f]{64}");
        // tax year, calc id, user id passthrough
        assertThat(line.get("taxYear").asText()).isEqualTo(TEST_TAX_YEAR_LABEL);
        assertThat(line.get("calculationId").asText()).isEqualTo(TEST_CALC_ID);
        assertThat(line.get("userId").asText()).isEqualTo("user-test");
    }

    @Test
    @DisplayName("must NEVER contain plaintext NINO in the log file (GDPR data minimisation)")
    void shouldNeverContainPlaintextNino() throws IOException {
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, TEST_CALC_ID);

        String contents = Files.readString(logFile, StandardCharsets.UTF_8);

        assertThat(contents).doesNotContain(TEST_NINO);
        assertThat(contents).doesNotContain("AA123456");
        // And the field name must not be present either
        assertThat(contents).doesNotContain("\"nino\":");
    }

    @Test
    @DisplayName("should produce stable submission hash for same (nino, taxYear, calcId)")
    void shouldProduceStableSubmissionHash() throws IOException {
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, TEST_CALC_ID);
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, TEST_CALC_ID);

        List<String> lines = Files.readAllLines(logFile);
        JsonNode l1 = objectMapper.readTree(lines.get(0));
        JsonNode l2 = objectMapper.readTree(lines.get(1));

        assertThat(l1.get("submissionHashSha256").asText())
            .isEqualTo(l2.get("submissionHashSha256").asText());
    }

    @Test
    @DisplayName("should produce different submission hash when calculationId differs")
    void shouldProduceDifferentHashForDifferentCalcId() throws IOException {
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, "calc-A");
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, "calc-B");

        List<String> lines = Files.readAllLines(logFile);
        JsonNode l1 = objectMapper.readTree(lines.get(0));
        JsonNode l2 = objectMapper.readTree(lines.get(1));

        assertThat(l1.get("submissionHashSha256").asText())
            .isNotEqualTo(l2.get("submissionHashSha256").asText());
    }

    @Test
    @DisplayName("should produce different submission hash when nino differs (per-NINO domain separation)")
    void shouldProduceDifferentHashForDifferentNino() throws IOException {
        auditLog.recordConfirmedSubmission(CONFIRMATION, "AA111111A", TEST_TAX_YEAR_LABEL, TEST_CALC_ID);
        auditLog.recordConfirmedSubmission(CONFIRMATION, "AA222222B", TEST_TAX_YEAR_LABEL, TEST_CALC_ID);

        List<String> lines = Files.readAllLines(logFile);
        JsonNode l1 = objectMapper.readTree(lines.get(0));
        JsonNode l2 = objectMapper.readTree(lines.get(1));

        assertThat(l1.get("submissionHashSha256").asText())
            .as("submission hash MUST bind to the NINO so a swapped NINO is detectable")
            .isNotEqualTo(l2.get("submissionHashSha256").asText());
    }

    @Test
    @DisplayName("should produce different submission hash when taxYear differs (per-year domain separation)")
    void shouldProduceDifferentHashForDifferentTaxYear() throws IOException {
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, "2024-25", TEST_CALC_ID);
        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, "2025-26", TEST_CALC_ID);

        List<String> lines = Files.readAllLines(logFile);
        JsonNode l1 = objectMapper.readTree(lines.get(0));
        JsonNode l2 = objectMapper.readTree(lines.get(1));

        assertThat(l1.get("submissionHashSha256").asText())
            .as("submission hash MUST bind to the tax year so a cross-year replay is detectable")
            .isNotEqualTo(l2.get("submissionHashSha256").asText());
    }

    @Test
    @DisplayName("should reject null confirmation, blank NINO, blank taxYear, blank calcId")
    void shouldRejectInvalidInputs() {
        assertThatThrownBy(() -> auditLog.recordConfirmedSubmission(null, TEST_NINO, TEST_TAX_YEAR_LABEL, TEST_CALC_ID))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> auditLog.recordConfirmedSubmission(CONFIRMATION, "", TEST_TAX_YEAR_LABEL, TEST_CALC_ID))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, "", TEST_CALC_ID))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should create parent directories on first write")
    void shouldCreateParentDirectoriesLazily() throws IOException {
        // Confirm log file does not exist initially
        assertThat(Files.exists(logFile)).isFalse();

        auditLog.recordConfirmedSubmission(CONFIRMATION, TEST_NINO, TEST_TAX_YEAR_LABEL, TEST_CALC_ID);

        assertThat(Files.exists(logFile)).isTrue();
        assertThat(Files.exists(logFile.getParent())).isTrue();
    }
}
