package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;

import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A stored refresh token must only be deleted when HMRC actually rejects it. Deleting it on a
 * transient failure (network, timeout, server error) forces the user through a full
 * re-authentication over a momentary blip, so the distinction is load-bearing for data safety.
 */
@DisplayName("OAuth refresh rejection policy")
class OAuthRefreshRejectionTest {

    @Test
    @DisplayName("treats invalid_grant as a genuine rejection")
    void invalidGrantIsRejection() {
        assertThat(OAuthServiceFactory.isRefreshTokenRejected(
            new HmrcOAuthException(OAuthError.INVALID_GRANT))).isTrue();
    }

    @Test
    @DisplayName("sees an invalid_grant wrapped in a CompletionException")
    void unwrapsWrappedRejection() {
        Throwable wrapped = new CompletionException(new HmrcOAuthException(OAuthError.INVALID_GRANT));
        assertThat(OAuthServiceFactory.isRefreshTokenRejected(wrapped)).isTrue();
    }

    @Test
    @DisplayName("does not treat a timeout or server error as a rejection")
    void transientFailuresAreNotRejections() {
        assertThat(OAuthServiceFactory.isRefreshTokenRejected(
            new HmrcOAuthException(OAuthError.TIMEOUT))).isFalse();
        assertThat(OAuthServiceFactory.isRefreshTokenRejected(
            new HmrcOAuthException(OAuthError.SERVER_ERROR))).isFalse();
        assertThat(OAuthServiceFactory.isRefreshTokenRejected(
            new CompletionException(new java.net.SocketTimeoutException("read timed out")))).isFalse();
    }

    @Test
    @DisplayName("treats an unavailable master key as transient, not a rejection")
    void masterKeyUnavailableIsNotRejection() {
        assertThat(OAuthServiceFactory.isRefreshTokenRejected(
            new MasterKeyUnavailableException("master key unreadable"))).isFalse();
    }
}
