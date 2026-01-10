package uk.selfemploy.hmrc.oauth.storage;

import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.util.Optional;

/**
 * Interface for secure storage of OAuth tokens.
 * Implementations must ensure tokens are stored securely and never exposed in logs.
 */
public interface TokenStorage {

    /**
     * Saves OAuth tokens to secure storage.
     *
     * @param tokens The tokens to store
     * @throws TokenStorageException if storage fails
     */
    void save(OAuthTokens tokens) throws TokenStorageException;

    /**
     * Loads OAuth tokens from secure storage.
     *
     * @return Optional containing tokens if found, empty otherwise
     * @throws TokenStorageException if loading fails
     */
    Optional<OAuthTokens> load() throws TokenStorageException;

    /**
     * Deletes OAuth tokens from secure storage.
     *
     * @throws TokenStorageException if deletion fails
     */
    void delete() throws TokenStorageException;

    /**
     * Checks if tokens exist in storage.
     *
     * @return true if tokens are stored
     */
    boolean exists();

    /**
     * Returns the storage type name for logging/diagnostics.
     *
     * @return Storage type name (e.g., "macOS Keychain", "Encrypted File")
     */
    String getStorageType();
}
