package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.security.RateLimitedException;
import uk.selfemploy.ui.service.security.WrongPassphraseException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How an unlock failure is reported. A real fault (an unreadable vault, a crypto provider problem) must
 * not be shown as "wrong passphrase", or a user would keep retyping a passphrase that was never wrong.
 */
@DisplayName("AppUnlockViewModel - unlock failure messages")
class AppUnlockViewModelTest {

    private AppUnlockViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new AppUnlockViewModel();
    }

    @Test
    @DisplayName("a wrong secret reports the wrong-passphrase message")
    void wrongPassphrase() {
        assertThat(viewModel.errorFor(new WrongPassphraseException()).key())
                .isEqualTo("unlock.error.wrong");
    }

    @Test
    @DisplayName("a rate-limit reports the remaining wait as an argument")
    void rateLimited() {
        AppUnlockViewModel.ErrorMessage error = viewModel.errorFor(new RateLimitedException(4000));
        assertThat(error.key()).isEqualTo("unlock.error.rateLimited");
        assertThat(error.args()).containsExactly(4L);
    }

    @Test
    @DisplayName("the remaining wait rounds up, so the countdown never reads zero while still locked")
    void retrySecondsRoundUp() {
        assertThat(viewModel.retryAfterSeconds(1)).isEqualTo(1);
        assertThat(viewModel.retryAfterSeconds(999)).isEqualTo(1);
        assertThat(viewModel.retryAfterSeconds(1000)).isEqualTo(1);
        assertThat(viewModel.retryAfterSeconds(1001)).isEqualTo(2);
        assertThat(viewModel.retryAfterSeconds(30_000)).isEqualTo(30);
    }

    @Test
    @DisplayName("any other failure falls back to the generic message, never 'wrong passphrase'")
    void otherFailuresAreGeneric() {
        assertThat(viewModel.errorFor(new IOException("vault unreadable")).key())
                .isEqualTo("unlock.error.generic");
        assertThat(viewModel.errorFor(new IllegalStateException("no AES provider")).key())
                .isEqualTo("unlock.error.generic");
        assertThat(viewModel.errorFor(null).key()).isEqualTo("unlock.error.generic");
    }
}
