package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The refresh token is a long-lived credential for the taxpayer's HMRC account. It was previously
 * written to the settings table in the clear, so anyone able to read the database file — another
 * local user, a backup, a sync folder — could impersonate the taxpayer to HMRC. These tests pin
 * the token to being unreadable in the stored file.
 */
@DisplayName("OAuth tokens at rest")
class TokenEncryptionAtRestTest {

    private static final String ACCESS_TOKEN = "access-token-abc123";
    private static final String REFRESH_TOKEN = "refresh-token-xyz789";

    @TempDir
    Path tempDir;

    /**
     * Binds encryption to a master key inside the test's temp directory, so the test neither reads
     * nor writes the real per-user {@code master.key} and passes on a locked-down CI home.
     */
    private CredentialEncryption tempKeyEncryption() {
        return new CredentialEncryption(new MasterKeyProvider(tempDir.resolve("master.key")));
    }

    private SqliteDataStore store(Path dbPath) {
        return new SqliteDataStore(dbPath, tempKeyEncryption());
    }

    @Test
    @DisplayName("round-trips the tokens through storage")
    void roundTripsTokens() {
        SqliteDataStore store = store(tempDir.resolve("tokens.db"));

        store.saveOAuthTokens(ACCESS_TOKEN, REFRESH_TOKEN, 3600, "bearer", "read:self-assessment",
            Instant.parse("2026-01-01T00:00:00Z"));

        String[] tokens = store.loadOAuthTokens();
        assertThat(tokens[0]).isEqualTo(ACCESS_TOKEN);
        assertThat(tokens[1]).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("does not leave the tokens readable in the database file")
    void doesNotStoreTokensInClear() throws Exception {
        Path dbPath = tempDir.resolve("tokens.db");
        SqliteDataStore store = store(dbPath);

        store.saveOAuthTokens(ACCESS_TOKEN, REFRESH_TOKEN, 3600, "bearer", "read:self-assessment",
            Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(storedSetting(dbPath, "oauth_refresh_token"))
            .isNotNull()
            .doesNotContain(REFRESH_TOKEN);
        assertThat(storedSetting(dbPath, "oauth_access_token"))
            .isNotNull()
            .doesNotContain(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("rewrites a token left in the clear by an earlier version")
    void reEncryptsPlaintextTokenFromEarlierVersion() throws Exception {
        Path dbPath = tempDir.resolve("tokens.db");
        SqliteDataStore store = store(dbPath);
        store.saveOAuthTokens(ACCESS_TOKEN, REFRESH_TOKEN, 3600, "bearer", "read", Instant.now());

        writeSettingDirectly(dbPath, "oauth_access_token", ACCESS_TOKEN);
        writeSettingDirectly(dbPath, "oauth_refresh_token", REFRESH_TOKEN);

        String[] tokens = store.loadOAuthTokens();

        assertThat(tokens[0]).isEqualTo(ACCESS_TOKEN);
        assertThat(tokens[1]).isEqualTo(REFRESH_TOKEN);
        assertThat(storedSetting(dbPath, "oauth_refresh_token")).doesNotContain(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("keeps the encrypted tokens intact when the master key is unavailable")
    void keepsEncryptedTokensWhenMasterKeyUnavailable() throws Exception {
        Path dbPath = tempDir.resolve("tokens.db");
        store(dbPath).saveOAuthTokens(ACCESS_TOKEN, REFRESH_TOKEN, 3600, "bearer", "read",
            Instant.parse("2026-01-01T00:00:00Z"));
        String ciphertextBefore = storedSetting(dbPath, "oauth_refresh_token");

        CredentialEncryption keyGone = new CredentialEncryption(
            new MasterKeyProvider(tempDir.resolve("master.key"))) {
            @Override
            public String decrypt(String encoded) {
                throw new MasterKeyUnavailableException("master key temporarily unreadable");
            }
        };
        String[] tokens = new SqliteDataStore(dbPath, keyGone).loadOAuthTokens();

        assertThat(tokens).isNull();
        assertThat(storedSetting(dbPath, "oauth_refresh_token")).isEqualTo(ciphertextBefore);
    }

    @Test
    @DisplayName("keeps a plaintext token when re-encryption fails, rather than discarding it")
    void keepsLegacyTokenWhenReEncryptionFails() throws Exception {
        Path dbPath = tempDir.resolve("tokens.db");
        CredentialEncryption failingEncrypt = new CredentialEncryption(
            new MasterKeyProvider(tempDir.resolve("master.key"))) {
            @Override
            public String encrypt(String plaintext) {
                throw new CredentialEncryptionException("master key temporarily unwritable");
            }
        };
        SqliteDataStore store = new SqliteDataStore(dbPath, failingEncrypt);
        writeSettingDirectly(dbPath, "oauth_access_token", ACCESS_TOKEN);
        writeSettingDirectly(dbPath, "oauth_refresh_token", REFRESH_TOKEN);

        String[] tokens = store.loadOAuthTokens();

        assertThat(tokens[0]).isEqualTo(ACCESS_TOKEN);
        assertThat(tokens[1]).isEqualTo(REFRESH_TOKEN);
        assertThat(storedSetting(dbPath, "oauth_access_token")).isEqualTo(ACCESS_TOKEN);
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
