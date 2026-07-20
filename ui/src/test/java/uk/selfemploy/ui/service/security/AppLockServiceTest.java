package uk.selfemploy.ui.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AppLockService - passphrase key-wrapping, unlock, recovery, rate limiting")
class AppLockServiceTest {

    @TempDir
    Path dir;

    private Path vault;
    private AtomicLong clock;
    private AppLockService service;

    @BeforeEach
    void setUp() {
        vault = dir.resolve("selfemploy.vault");
        clock = new AtomicLong(0);
        service = new AppLockService(vault, clock::get);
    }

    private static char[] pw(String s) {
        return s.toCharArray();
    }

    @Test
    @DisplayName("enable then unlock returns the same database key; vault is written 0600")
    void enableThenUnlock() throws Exception {
        assertThat(service.isProtectionEnabled()).isFalse();

        AppLockService.EnableResult enabled = service.enableProtection(pw("correct horse battery"));
        assertThat(service.isProtectionEnabled()).isTrue();
        assertThat(enabled.recoveryCode()).matches("[A-Z2-7]{5}(-[A-Z2-7]{5}){4}");

        DbKey unlocked = service.unlock(pw("correct horse battery"));
        assertThat(unlocked.hex()).isEqualTo(enabled.dbKey().hex());
        assertThat(unlocked.hex()).hasSize(64);

        if (Files.getFileStore(vault).supportsFileAttributeView(java.nio.file.attribute.PosixFileAttributeView.class)) {
            assertThat(Files.getPosixFilePermissions(vault)).containsExactlyInAnyOrder(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE);
        }
    }

    @Test
    @DisplayName("wrong passphrase fails with WrongPassphraseException")
    void wrongPassphrase() throws Exception {
        service.enableProtection(pw("the-right-one"));
        assertThatThrownBy(() -> service.unlock(pw("the-wrong-one")))
                .isInstanceOf(WrongPassphraseException.class);
    }

    @Test
    @DisplayName("the recovery code unlocks the same database key as the passphrase")
    void recoveryCodeUnlocks() throws Exception {
        AppLockService.EnableResult enabled = service.enableProtection(pw("my passphrase"));
        DbKey viaRecovery = service.unlockWithRecovery(enabled.recoveryCode().toCharArray());
        assertThat(viaRecovery.hex()).isEqualTo(enabled.dbKey().hex());
    }

    @Test
    @DisplayName("changing the passphrase re-wraps the same key: new works, old fails, key unchanged")
    void changePassphrase() throws Exception {
        AppLockService.EnableResult enabled = service.enableProtection(pw("old-pass"));
        service.changePassphrase(pw("old-pass"), pw("new-pass"));

        DbKey viaNew = service.unlock(pw("new-pass"));
        assertThat(viaNew.hex()).isEqualTo(enabled.dbKey().hex());
        assertThatThrownBy(() -> service.unlock(pw("old-pass")))
                .isInstanceOf(WrongPassphraseException.class);
        // The recovery code still opens the same key (it was not re-wrapped).
        assertThat(service.unlockWithRecovery(enabled.recoveryCode().toCharArray()).hex())
                .isEqualTo(enabled.dbKey().hex());
    }

    @Test
    @DisplayName("repeated failures trigger an escalating rate-limit that clears after the delay")
    void rateLimiting() throws Exception {
        service.enableProtection(pw("real"));

        // Three free wrong attempts, then a fourth arms the limiter.
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.unlock(pw("nope")))
                    .isInstanceOf(WrongPassphraseException.class);
        }
        // The next attempt is rate-limited (not even evaluated).
        assertThatThrownBy(() -> service.unlock(pw("real")))
                .isInstanceOf(RateLimitedException.class);

        // After the delay elapses, attempts are accepted again.
        clock.addAndGet(31_000);
        DbKey key = service.unlock(pw("real"));
        assertThat(key.hex()).hasSize(64);
    }

    @Test
    @DisplayName("a tampered KDF parameter (AAD mismatch) fails the unlock")
    void aadTamperFails() throws Exception {
        service.enableProtection(pw("secret"));

        // Corrupt the stored iterations so the AAD no longer matches what was sealed.
        String json = Files.readString(vault);
        String tampered = json.replaceFirst("\"iterations\":3", "\"iterations\":4");
        assertThat(tampered).isNotEqualTo(json);
        Files.writeString(vault, tampered);

        assertThatThrownBy(() -> service.unlock(pw("secret")))
                .isInstanceOf(WrongPassphraseException.class);
    }
}
