package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Opening a protected database before the unlock gate has run.
 *
 * <p>Without a key, SQLCipher reads the encrypted file as garbage and reports "file is not a database" —
 * which points at the file when the real fault is that something ran before the unlock. The store checks
 * for the vault beside <em>its own</em> database, so a store opened elsewhere is unaffected by whether
 * the person running the tests has protection switched on.
 */
@DisplayName("SqliteDataStore - opening a protected database with no key provisioned")
class SqliteDataStoreKeyProvisioningTest {

    @TempDir
    Path dir;

    @Test
    @DisplayName("a vault beside the database and no key is refused, naming the real cause")
    void refusesToOpenWhenAVaultExistsButNoKeyWasProvisioned() throws Exception {
        Path db = dir.resolve("selfemploy.db");
        Files.writeString(dir.resolve("selfemploy.vault"), "{\"version\":1}");

        assertThatThrownBy(() -> new SqliteDataStore(db))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unlock");
    }

    @Test
    @DisplayName("an unprotected database still opens, since no vault stands beside it")
    void opensNormallyWhenThereIsNoVault() {
        Path db = dir.resolve("selfemploy.db");

        assertThatCode(() -> new SqliteDataStore(db).close()).doesNotThrowAnyException();
        assertThat(db).exists();
    }

    @Test
    @DisplayName("another database's vault is not this database's business")
    void aVaultElsewhereDoesNotBlockThisStore() throws Exception {
        // The guard used to consult the logged-in user's vault path, so switching protection on made
        // every temporary store throw - the machine's state leaking into unrelated databases.
        Path elsewhere = Files.createDirectory(dir.resolve("other"));
        Files.writeString(elsewhere.resolve("selfemploy.vault"), "{\"version\":1}");

        Path db = dir.resolve("selfemploy.db");
        assertThatCode(() -> new SqliteDataStore(db).close()).doesNotThrowAnyException();
    }
}
