package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules guarding the "protect your data" step. Enabling protection is irreversible without the
 * passphrase or the recovery code, so these are the checks that stand between a user and data they
 * can never open again.
 */
@DisplayName("AppProtectViewModel - passphrase rules and the commit gate")
class AppProtectViewModelTest {

    private AppProtectViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new AppProtectViewModel();
    }

    @Test
    @DisplayName("a matching passphrase of at least the minimum length is accepted")
    void acceptsValidPassphrase() {
        assertThat(viewModel.validate("river-otter-sunrise", "river-otter-sunrise"))
                .isEqualTo(AppProtectViewModel.Validation.OK);
    }

    @Test
    @DisplayName("the minimum length is a boundary: one short is rejected, exactly the minimum is accepted")
    void enforcesMinimumLengthAtTheBoundary() {
        String tooShort = "a".repeat(AppProtectViewModel.MIN_PASSPHRASE_LENGTH - 1);
        String exact = "a".repeat(AppProtectViewModel.MIN_PASSPHRASE_LENGTH);

        assertThat(viewModel.validate(tooShort, tooShort))
                .isEqualTo(AppProtectViewModel.Validation.TOO_SHORT);
        assertThat(viewModel.validate(exact, exact))
                .isEqualTo(AppProtectViewModel.Validation.OK);
    }

    @Test
    @DisplayName("an empty passphrase is rejected as too short, not accepted")
    void rejectsEmptyPassphrase() {
        assertThat(viewModel.validate("", ""))
                .isEqualTo(AppProtectViewModel.Validation.TOO_SHORT);
    }

    @Test
    @DisplayName("a mismatched confirmation is rejected")
    void rejectsMismatchedConfirmation() {
        assertThat(viewModel.validate("river-otter-sunrise", "river-otter-sunrize"))
                .isEqualTo(AppProtectViewModel.Validation.MISMATCH);
    }

    @Test
    @DisplayName("a too-short passphrase reports its length, not a mismatch, even when the pair differs")
    void lengthIsReportedBeforeMismatch() {
        assertThat(viewModel.validate("short", "different"))
                .isEqualTo(AppProtectViewModel.Validation.TOO_SHORT);
    }

    @Test
    @DisplayName("each rejection carries its message; only the length message formats an argument")
    void rejectionsCarryTheirMessages() {
        assertThat(AppProtectViewModel.Validation.TOO_SHORT.messageKey()).isEqualTo("protect.error.tooShort");
        assertThat(AppProtectViewModel.Validation.TOO_SHORT.messageArgs())
                .containsExactly(AppProtectViewModel.MIN_PASSPHRASE_LENGTH);
        assertThat(AppProtectViewModel.Validation.MISMATCH.messageKey()).isEqualTo("protect.error.mismatch");
        assertThat(AppProtectViewModel.Validation.MISMATCH.messageArgs()).isEmpty();
        assertThat(AppProtectViewModel.Validation.OK.messageKey()).isNull();
    }

    @Test
    @DisplayName("the vault is only committed once a recovery code has been shown AND acknowledged")
    void commitRequiresAnAcknowledgedRecoveryCode() {
        assertThat(viewModel.canCommit(true, true)).isTrue();
        assertThat(viewModel.canCommit(true, false)).isFalse();
        assertThat(viewModel.canCommit(false, true)).isFalse();
        assertThat(viewModel.canCommit(false, false)).isFalse();
    }
}
