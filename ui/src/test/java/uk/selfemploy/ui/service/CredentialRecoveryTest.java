package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The HMRC client id and secret are entered once and encrypted at rest. A transient master-key
 * problem (a restored-but-not-yet-present key file, a momentarily unwritable home) must not be
 * mistaken for corruption and cause the stored ciphertext to be wiped, or the user would have to
 * re-enter their Developer Hub credentials after a glitch that fixes itself.
 */
@DisplayName("HMRC credential recovery")
class CredentialRecoveryTest {

    @TempDir
    Path tempDir;

    private CredentialEncryption tempKeyEncryption() {
        return new CredentialEncryption(new MasterKeyProvider(tempDir.resolve("master.key")));
    }

    @Test
    @DisplayName("keeps the encrypted client secret when the master key is unavailable")
    void keepsClientSecretWhenMasterKeyUnavailable() throws Exception {
        Path dbPath = tempDir.resolve("creds.db");
        new SqliteDataStore(dbPath, tempKeyEncryption()).saveHmrcClientSecret("developer-hub-secret");
        String ciphertextBefore = storedSetting(dbPath, "hmrc_client_secret_enc");

        CredentialEncryption keyGone = new CredentialEncryption(
            new MasterKeyProvider(tempDir.resolve("master.key"))) {
            @Override
            public String decrypt(String encoded) {
                throw new MasterKeyUnavailableException("master key temporarily unreadable");
            }
        };
        String loaded = new SqliteDataStore(dbPath, keyGone).loadHmrcClientSecret();

        assertThat(loaded).isNull();
        assertThat(storedSetting(dbPath, "hmrc_client_secret_enc")).isEqualTo(ciphertextBefore);
    }

    @Test
    @DisplayName("reports an undecryptable client secret as absent but does not delete it")
    void preservesUndecryptableClientSecret() throws Exception {
        Path dbPath = tempDir.resolve("creds.db");
        SqliteDataStore store = new SqliteDataStore(dbPath, tempKeyEncryption());
        store.saveHmrcClientSecret("developer-hub-secret");

        writeSettingDirectly(dbPath, "hmrc_client_secret_enc", "v2:not-valid-base64-ciphertext!!");

        assertThat(store.loadHmrcClientSecret()).isNull();
        assertThat(storedSetting(dbPath, "hmrc_client_secret_enc"))
            .isEqualTo("v2:not-valid-base64-ciphertext!!");
    }

    private String storedSetting(Path dbPath, String key) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement statement =
                 connection.prepareStatement("SELECT value FROM settings WHERE key = ?")) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private void writeSettingDirectly(Path dbPath, String key, String value) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)")) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }
}
