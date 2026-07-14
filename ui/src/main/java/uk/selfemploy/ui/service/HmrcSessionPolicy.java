package uk.selfemploy.ui.service;

import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.util.logging.Logger;

/**
 * The single place that decides the fate of a stored HMRC session when a token refresh fails, and the
 * only code allowed to destroy one.
 *
 * <p>Every refresh call site used to make this decision for itself, and they disagreed. Most deleted
 * every stored token on <em>any</em> exception, so a momentary timeout during a tax calculation cost
 * the user an eighteen-month refresh token and a full browser re-authentication. The rule is now
 * stated once, here, and applied everywhere.
 *
 * <p>The rule: a credential is destroyed only when HMRC has actually refused it. A rejection —
 * {@code invalid_grant} — is HMRC's own word that the grant is dead, and the session goes. Anything
 * else proves nothing about the stored credentials: a timeout, an outage, a session that could not be
 * decrypted, or simply having no refresh token to send. Those leave the session untouched, so the next
 * attempt can succeed.
 *
 * <p>Database and in-memory session are always cleared together. A dead refresh token left in memory
 * would be presented again on the next call, and rejected again.
 */
final class HmrcSessionPolicy {

    private static final Logger LOG = Logger.getLogger(HmrcSessionPolicy.class.getName());

    private HmrcSessionPolicy() {
        // Utility class
    }

    /**
     * Records a refreshed session.
     *
     * <p>The write is reported when it fails rather than passing silently. HMRC invalidates a refresh
     * token the moment it issues a new one, so a rotation that is not written down leaves the spent
     * token as the only copy on disk — and the next start presents it, is told {@code invalid_grant},
     * and the user is logged out by a local storage failure rather than by HMRC.
     *
     * @param tokens the refreshed session
     */
    static void persistRefreshedTokens(OAuthTokens tokens) {
        boolean saved = SqliteDataStore.getInstance().saveOAuthTokens(
            tokens.accessToken(),
            tokens.refreshToken(),
            tokens.expiresIn(),
            tokens.tokenType(),
            tokens.scope(),
            tokens.issuedAt());

        if (saved) {
            LOG.info("OAuth tokens refreshed and persisted (expires in "
                + tokens.getSecondsUntilExpiry() + "s)");
        } else {
            LOG.warning("The refreshed HMRC session could not be saved. It works for this run, but "
                + "the stored session is now stale and a reconnect may be needed after a restart");
        }
    }

    /**
     * Applies the policy to a failed refresh and reports whether the stored session survived.
     *
     * @param error the failure the refresh attempt produced
     * @param oauthService the service holding the in-memory session, cleared with the database. Passed
     *     in rather than taken from {@link OAuthServiceFactory} because that factory may still be
     *     constructing the service when this is called.
     * @return true if the stored session was cleared, false if it was left intact
     */
    static boolean onRefreshFailure(Throwable error, HmrcOAuthService oauthService) {
        if (!OAuthServiceFactory.isRefreshTokenRejected(error)) {
            LOG.warning("Could not renew the HMRC session (" + error.getMessage()
                + "); the stored tokens are untouched");
            return false;
        }

        LOG.warning("HMRC rejected the refresh token; clearing the stored session");
        SqliteDataStore.getInstance().clearOAuthTokens();
        oauthService.setTokens(null);
        return true;
    }
}
