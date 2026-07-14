package uk.selfemploy.hmrc.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * A PKCE (RFC 7636) code verifier and its S256 challenge.
 *
 * <p>PKCE is required for native/public OAuth clients (RFC 8252). It binds the authorization code
 * to the client instance that requested it: the challenge travels on the authorization request,
 * the verifier only on the token exchange. An authorization code intercepted in transit — for
 * example by a process that has taken the loopback callback port — is therefore not redeemable
 * without the verifier, which never leaves this process.</p>
 *
 * @param verifier  the high-entropy secret retained by this client and sent only on token exchange
 * @param challenge {@code BASE64URL(SHA-256(verifier))}, sent on the authorization request
 */
public record PkceChallenge(String verifier, String challenge) {

    /** The challenge method this class implements, sent as {@code code_challenge_method}. */
    public static final String METHOD = "S256";

    private static final int VERIFIER_BYTES = 32;

    /**
     * Generates a fresh verifier/challenge pair.
     *
     * @param secureRandom the CSPRNG used to generate the verifier
     * @return a new PKCE pair
     */
    public static PkceChallenge generate(SecureRandom secureRandom) {
        byte[] bytes = new byte[VERIFIER_BYTES];
        secureRandom.nextBytes(bytes);
        String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new PkceChallenge(verifier, challengeFor(verifier));
    }

    /**
     * Derives the S256 challenge for a verifier: {@code BASE64URL(SHA-256(verifier))}.
     *
     * @param verifier the code verifier
     * @return the challenge to send on the authorization request
     */
    public static String challengeFor(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every Java platform", e);
        }
    }
}
