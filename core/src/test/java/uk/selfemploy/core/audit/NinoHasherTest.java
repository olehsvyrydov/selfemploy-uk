package uk.selfemploy.core.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD tests for {@link NinoHasher} — SLFEMPUK-35 / S17-11.
 */
@DisplayName("NinoHasher (SLFEMPUK-35)")
class NinoHasherTest {

    private static final String TEST_NINO = "AA123456A";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should produce deterministic hex SHA-256 for the same NINO with the same salt")
    void shouldBeDeterministicForSameSalt() throws IOException {
        Path saltFile = tempDir.resolve("salt");
        NinoHasher hasher = new NinoHasher(saltFile);

        String h1 = hasher.hash(TEST_NINO);
        String h2 = hasher.hash(TEST_NINO);

        assertThat(h1).isEqualTo(h2);
        assertThat(h1).hasSize(64); // SHA-256 hex
        assertThat(h1).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("should never leak plaintext NINO in the output")
    void shouldNotContainPlaintextNino() throws IOException {
        Path saltFile = tempDir.resolve("salt");
        NinoHasher hasher = new NinoHasher(saltFile);

        String hash = hasher.hash(TEST_NINO);

        assertThat(hash).doesNotContain(TEST_NINO);
        assertThat(hash).doesNotContain("AA123456");
    }

    @Test
    @DisplayName("should generate fresh 16-byte salt on first use and persist it")
    void shouldGenerateAndPersistFreshSalt() throws IOException {
        Path saltFile = tempDir.resolve("subdir").resolve("salt");
        assertThat(Files.exists(saltFile)).isFalse();

        new NinoHasher(saltFile);

        assertThat(Files.exists(saltFile)).isTrue();
        assertThat(Files.readAllBytes(saltFile)).hasSize(16);
    }

    @Test
    @DisplayName("should reuse existing salt file across instances (per-install)")
    void shouldReuseExistingSalt() throws IOException {
        Path saltFile = tempDir.resolve("salt");
        NinoHasher first = new NinoHasher(saltFile);
        String hash1 = first.hash(TEST_NINO);

        NinoHasher second = new NinoHasher(saltFile);
        String hash2 = second.hash(TEST_NINO);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("should produce different hashes for different per-install salts")
    void shouldProduceDifferentHashesForDifferentSalts() throws IOException {
        Path saltA = tempDir.resolve("installA").resolve("salt");
        Path saltB = tempDir.resolve("installB").resolve("salt");

        NinoHasher a = new NinoHasher(saltA);
        NinoHasher b = new NinoHasher(saltB);

        assertThat(a.hash(TEST_NINO)).isNotEqualTo(b.hash(TEST_NINO));
    }

    @Test
    @DisplayName("should produce different hashes for different NINOs")
    void shouldProduceDifferentHashesForDifferentNinos() throws IOException {
        Path saltFile = tempDir.resolve("salt");
        NinoHasher hasher = new NinoHasher(saltFile);

        assertThat(hasher.hash("AA111111A")).isNotEqualTo(hasher.hash("AA222222B"));
    }

    @Test
    @DisplayName("should reject null or blank NINO")
    void shouldRejectNullOrBlankNino() throws IOException {
        Path saltFile = tempDir.resolve("salt");
        NinoHasher hasher = new NinoHasher(saltFile);

        assertThatThrownBy(() -> hasher.hash(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> hasher.hash(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> hasher.hash("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject salt file with unexpected length")
    void shouldRejectCorruptSalt() throws IOException {
        Path saltFile = tempDir.resolve("salt");
        Files.write(saltFile, HexFormat.of().parseHex("deadbeef")); // 4 bytes, not 16

        assertThatThrownBy(() -> new NinoHasher(saltFile))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unexpected length");
    }
}
