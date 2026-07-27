package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the Security section is allowed to claim. Enabling protection writes the key vault but defers
 * the encryption to the next launch, so there is a window in which a vault exists over a database that
 * is still plaintext — and the app must not report that as protected.
 */
@DisplayName("SecuritySettingsViewModel - honest protection status")
class SecuritySettingsViewModelTest {

    private SecuritySettingsViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new SecuritySettingsViewModel();
    }

    @Test
    @DisplayName("no vault means protection is off")
    void noVaultIsOff() {
        assertThat(viewModel.status(false, true)).isEqualTo(SecuritySettingsViewModel.ProtectionStatus.OFF);
    }

    @Test
    @DisplayName("a vault over a still-plaintext database reports pending, never on")
    void vaultOverPlaintextIsPending() {
        assertThat(viewModel.status(true, true))
                .isEqualTo(SecuritySettingsViewModel.ProtectionStatus.PENDING_RESTART);
    }

    @Test
    @DisplayName("a vault over an encrypted database is on")
    void vaultOverEncryptedIsOn() {
        assertThat(viewModel.status(true, false)).isEqualTo(SecuritySettingsViewModel.ProtectionStatus.ON);
    }

    @Test
    @DisplayName("an encrypted database with no vault is not reported as off-and-fine")
    void encryptedWithoutVaultStillReportsOff() {
        // Should not happen, but if the vault were lost the data is unreachable — reporting OFF is the
        // honest answer for the enable control, and the unlock gate is what surfaces the real problem.
        assertThat(viewModel.status(false, false)).isEqualTo(SecuritySettingsViewModel.ProtectionStatus.OFF);
    }

    @Test
    @DisplayName("protection can only be turned on from off")
    void enableOnlyFromOff() {
        assertThat(viewModel.canEnableProtection(SecuritySettingsViewModel.ProtectionStatus.OFF)).isTrue();
        assertThat(viewModel.canEnableProtection(SecuritySettingsViewModel.ProtectionStatus.PENDING_RESTART)).isFalse();
        assertThat(viewModel.canEnableProtection(SecuritySettingsViewModel.ProtectionStatus.ON)).isFalse();
    }

    @Test
    @DisplayName("passphrase and recovery controls work as soon as the vault exists, before the restart")
    void keyControlsAvailableOncePending() {
        assertThat(viewModel.canManageKeys(SecuritySettingsViewModel.ProtectionStatus.OFF)).isFalse();
        assertThat(viewModel.canManageKeys(SecuritySettingsViewModel.ProtectionStatus.PENDING_RESTART)).isTrue();
        assertThat(viewModel.canManageKeys(SecuritySettingsViewModel.ProtectionStatus.ON)).isTrue();
    }

    @Test
    @DisplayName("every status carries the copy that describes it")
    void everyStatusHasCopy() {
        for (SecuritySettingsViewModel.ProtectionStatus status : SecuritySettingsViewModel.ProtectionStatus.values()) {
            assertThat(status.labelKey()).isNotBlank();
            assertThat(status.descriptionKey()).isNotBlank();
        }
    }
}
