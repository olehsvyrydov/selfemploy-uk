package uk.selfemploy.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EnvLoader — key allowlist")
class EnvLoaderTest {

    private Path writeEnv(Path dir, String content) throws Exception {
        Path env = dir.resolve(".env");
        Files.writeString(env, content);
        return env;
    }

    @Test
    @DisplayName("applies allowlisted HMRC credential keys")
    void appliesAllowlistedKeys() throws Exception {
        String key = "HMRC_CLIENT_ID";
        try {
            System.clearProperty(key);
            Path env = writeEnv(java.nio.file.Files.createTempDirectory("env-ok"),
                key + "=abc123clientid");

            EnvLoader.load(env);

            assertThat(System.getProperty(key)).isEqualTo("abc123clientid");
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    @DisplayName("ignores a redirected HMRC token URL — the secret-exfil key")
    void ignoresUrlOverride(@TempDir Path dir) throws Exception {
        String key = "HMRC_TOKEN_URL";
        try {
            System.clearProperty(key);
            Path env = writeEnv(dir, key + "=https://attacker.example/collect");

            EnvLoader.load(env);

            assertThat(System.getProperty(key)).isNull();
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    @DisplayName("ignores dangerous JVM keys like a trust store or proxy override")
    void ignoresJvmPoisoningKeys(@TempDir Path dir) throws Exception {
        String trustStore = "javax.net.ssl.trustStore";
        String proxy = "https.proxyHost";
        try {
            System.clearProperty(trustStore);
            System.clearProperty(proxy);
            Path env = writeEnv(dir,
                trustStore + "=/tmp/evil.jks\n" + proxy + "=attacker.example");

            EnvLoader.load(env);

            assertThat(System.getProperty(trustStore)).isNull();
            assertThat(System.getProperty(proxy)).isNull();
        } finally {
            System.clearProperty(trustStore);
            System.clearProperty(proxy);
        }
    }
}
