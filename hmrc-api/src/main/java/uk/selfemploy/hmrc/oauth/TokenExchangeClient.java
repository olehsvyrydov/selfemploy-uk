package uk.selfemploy.hmrc.oauth;

import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.util.concurrent.CompletableFuture;

/**
 * Client for exchanging authorization codes and refresh tokens with HMRC.
 */
public interface TokenExchangeClient {

    /**
     * Exchanges an authorization code for access and refresh tokens.
     *
     * @param authorizationCode The authorization code from the OAuth callback
     * @return Future containing the tokens
     */
    CompletableFuture<OAuthTokens> exchangeCodeForTokens(String authorizationCode);

    /**
     * Refreshes an expired access token using a refresh token.
     *
     * @param refreshToken The refresh token
     * @return Future containing new tokens
     */
    CompletableFuture<OAuthTokens> refreshTokens(String refreshToken);
}
