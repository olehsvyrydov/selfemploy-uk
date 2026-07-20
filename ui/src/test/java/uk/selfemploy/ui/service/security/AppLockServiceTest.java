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

    /** Prepares and commits protection, returning the one-time recovery code. */
    private String enable(String passphrase) throws Exception {
        AppLockService.PendingProtection pending = service.prepareProtection(pw(passphrase));
        String code = pending.recoveryCode();
        pending.commit();
        return code;
    }

    @Test
    @DisplayName("prepare + commit enables protection and unlock returns a database key; vault is 0600")
    void enableThenUnlock() throws Exception {
        assertThat(service.isProtectionEnabled()).isFalse();

        String recovery = enable("correct horse battery");
        assertThat(service.isProtectionEnabled()).isTrue();
        assertThat(recovery).matches("[A-Z2-7]{5}(-[A-Z2-7]{5}){4}");

        DbKey unlocked = service.unlock(pw("correct horse battery"));
        assertThat(unlocked.hex()).hasSize(64);

        if (Files.getFileStore(vault).supportsFileAttributeView(java.nio.file.attribute.PosixFileAttributeView.class)) {
            assertThat(Files.getPosixFilePermissions(vault)).containsExactlyInAnyOrder(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE);
        }
    }

    @Test
    @DisplayName("preparing without committing does not enable protection (no vault written)")
    void prepareWithoutCommitLeavesUnprotected() {
        service.prepareProtection(pw("abandoned-passphrase"));   // no commit()
        assertThat(service.isProtectionEnabled()).isFalse();
        assertThat(Files.exists(vault)).isFalse();
    }

    @Test
    @DisplayName("wrong passphrase fails with WrongPassphraseException")
    void wrongPassphrase() throws Exception {
        enable("the-right-one");
        assertThatThrownBy(() -> service.unlock(pw("the-wrong-one")))
                .isInstanceOf(WrongPassphraseException.class);
    }

    @Test
    @DisplayName("the recovery code unlocks the same database key as the passphrase")
    void recoveryCodeUnlocks() throws Exception {
        String recovery = enable("my passphrase");
        DbKey viaPassphrase = service.unlock(pw("my passphrase"));
        DbKey viaRecovery = service.unlockWithRecovery(recovery.toCharArray());
        assertThat(viaRecovery.hex()).isEqualTo(viaPassphrase.hex());
    }

    @Test
    @DisplayName("changing the passphrase re-wraps the same key: new works, old fails, key unchanged")
    void changePassphrase() throws Exception {
        String recovery = enable("old-pass");
        DbKey before = service.unlock(pw("old-pass"));
        service.changePassphrase(pw("old-pass"), pw("new-pass"));

        assertThat(service.unlock(pw("new-pass")).hex()).isEqualTo(before.hex());
        assertThatThrownBy(() -> service.unlock(pw("old-pass")))
                .isInstanceOf(WrongPassphraseException.class);
        // The recovery code still opens the same key (it was not re-wrapped).
        assertThat(service.unlockWithRecovery(recovery.toCharArray()).hex()).isEqualTo(before.hex());
    }

    @Test
    @DisplayName("repeated failures trigger an escalating rate-limit that clears after the delay")
    void rateLimiting() throws Exception {
        enable("real");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.unlock(pw("nope")))
                    .isInstanceOf(WrongPassphraseException.class);
        }
        assertThatThrownBy(() -> service.unlock(pw("real")))
                .isInstanceOf(RateLimitedException.class);

        clock.addAndGet(31_000);
        assertThat(service.unlock(pw("real")).hex()).hasSize(64);
    }

    @Test
    @DisplayName("the rate-limit lockout survives a restart (a fresh service still throttles)")
    void rateLimitPersistsAcrossRestart() throws Exception {
        enable("real");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.unlock(pw("nope")))
                    .isInstanceOf(WrongPassphraseException.class);
        }
        // Simulate a relaunch with the same clock: the lockout is still in force.
        AppLockService restarted = new AppLockService(vault, clock::get);
        assertThatThrownBy(() -> restarted.unlock(pw("real")))
                .isInstanceOf(RateLimitedException.class);
    }

    @Test
    @DisplayName("a tampered KDF parameter (AAD mismatch) fails the unlock")
    void aadTamperFails() throws Exception {
        enable("secret");
        String json = Files.readString(vault);
        String tampered = json.replaceFirst("\"iterations\":3", "\"iterations\":4");
        assertThat(tampered).isNotEqualTo(json);
        Files.writeString(vault, tampered);

        assertThatThrownBy(() -> service.unlock(pw("secret")))
                .isInstanceOf(WrongPassphraseException.class);
    }
}
