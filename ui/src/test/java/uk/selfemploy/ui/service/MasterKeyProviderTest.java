package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The master key protects every credential and OAuth token at rest, so its lifecycle must be
 * conservative: generate once, hold it owner-only, and never silently replace a key that other
 * data already depends on.
 */
@DisplayName("MasterKeyProvider")
class MasterKeyProviderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("generates a 32-byte key and returns it stably across calls")
    void generatesAndCachesKey() {
        MasterKeyProvider provider = new MasterKeyProvider(tempDir.resolve("master.key"));

        byte[] first = provider.secret();
        byte[] second = provider.secret();

        assertThat(first).hasSize(32);
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("persists the same key for a new provider over the same file")
    void persistsKeyAcrossProviders() {
        Path keyPath = tempDir.resolve("master.key");

        byte[] created = new MasterKeyProvider(keyPath).secret();
        byte[] reloaded = new MasterKeyProvider(keyPath).secret();

        assertThat(reloaded).isEqualTo(created);
    }

    @Test
    @DisplayName("refuses to regenerate over a wrong-length key file, to avoid orphaning data")
    void refusesToRegenerateOverDamagedKey() throws Exception {
        Path keyPath = tempDir.resolve("master.key");
        Files.write(keyPath, new byte[]{1, 2, 3});

        assertThatThrownBy(() -> new MasterKeyProvider(keyPath).secret())
            .isInstanceOf(MasterKeyUnavailableException.class)
            .hasMessageContaining("unexpected length");

        assertThat(Files.readAllBytes(keyPath)).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("creates the key file readable only by its owner on POSIX systems")
    void createsOwnerOnlyKeyFile() throws Exception {
        Path keyPath = tempDir.resolve("master.key");
        new MasterKeyProvider(keyPath).secret();

        PosixFileAttributeView posix =
            Files.getFileAttributeView(keyPath, PosixFileAttributeView.class);
        if (posix == null) {
            return; // Non-POSIX filesystem (e.g. Windows): permissions are enforced via ACLs.
        }
        Set<PosixFilePermission> perms = posix.readAttributes().permissions();
        assertThat(perms).containsExactlyInAnyOrder(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    }
}
