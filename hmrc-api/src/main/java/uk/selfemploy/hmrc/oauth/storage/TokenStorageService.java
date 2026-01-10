package uk.selfemploy.hmrc.oauth.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Service for managing secure token storage.
 * Provides a unified interface for storing and retrieving OAuth tokens,
 * using the most appropriate storage mechanism available on the platform.
 */
@ApplicationScoped
public class TokenStorageService {

    private static final Logger log = LoggerFactory.getLogger(TokenStorageService.class);

    private final TokenStorage primaryStorage;
    private final TokenStorage fallbackStorage;

    @Inject
    public TokenStorageService(
            @ConfigProperty(name = "hmrc.storage.path", defaultValue = "") String storagePath) {

        // Determine storage path
        Path tokenFilePath = resolveStoragePath(storagePath);

        // For now, use encrypted file storage as primary
        // OS keychain integration can be added later
        this.fallbackStorage = new EncryptedFileTokenStorage(tokenFilePath);
        this.primaryStorage = this.fallbackStorage;

        log.info("Token storage initialized: {}", primaryStorage.getStorageType());
    }

    /**
     * Constructor for testing with custom storage implementations.
     */
    TokenStorageService(TokenStorage primaryStorage, TokenStorage fallbackStorage) {
        this.primaryStorage = primaryStorage;
        this.fallbackStorage = fallbackStorage;
    }

    /**
     * Saves OAuth tokens to secure storage.
     *
     * @param tokens The tokens to store
     */
    public void saveTokens(OAuthTokens tokens) {
        log.debug("Saving tokens to secure storage");

        try {
            primaryStorage.save(tokens);
            log.info("Tokens saved successfully using {}", primaryStorage.getStorageType());
        } catch (TokenStorageException e) {
            log.warn("Primary storage failed ({}), trying fallback", primaryStorage.getStorageType(), e);

            if (fallbackStorage != primaryStorage) {
                try {
                    fallbackStorage.save(tokens);
                    log.info("Tokens saved to fallback storage: {}", fallbackStorage.getStorageType());
                } catch (TokenStorageException fallbackEx) {
                    log.error("Fallback storage also failed", fallbackEx);
                    throw fallbackEx;
                }
            } else {
                throw e;
            }
        }
    }

    /**
     * Loads OAuth tokens from secure storage.
     *
     * @return Optional containing tokens if found, empty otherwise
     */
    public Optional<OAuthTokens> loadTokens() {
        log.debug("Loading tokens from secure storage");

        try {
            Optional<OAuthTokens> tokens = primaryStorage.load();
            if (tokens.isPresent()) {
                log.info("Tokens loaded from {}", primaryStorage.getStorageType());
                return tokens;
            }
        } catch (TokenStorageException e) {
            log.warn("Primary storage failed ({}), trying fallback", primaryStorage.getStorageType(), e);
        }

        // Try fallback if different from primary
        if (fallbackStorage != primaryStorage) {
            try {
                Optional<OAuthTokens> tokens = fallbackStorage.load();
                if (tokens.isPresent()) {
                    log.info("Tokens loaded from fallback: {}", fallbackStorage.getStorageType());
                    return tokens;
                }
            } catch (TokenStorageException fallbackEx) {
                log.warn("Fallback storage also failed", fallbackEx);
            }
        }

        log.debug("No tokens found in storage");
        return Optional.empty();
    }

    /**
     * Deletes OAuth tokens from all storage locations.
     */
    public void deleteTokens() {
        log.info("Deleting tokens from secure storage");

        try {
            primaryStorage.delete();
        } catch (TokenStorageException e) {
            log.warn("Failed to delete from primary storage", e);
        }

        if (fallbackStorage != primaryStorage) {
            try {
                fallbackStorage.delete();
            } catch (TokenStorageException e) {
                log.warn("Failed to delete from fallback storage", e);
            }
        }

        log.info("Tokens deleted from storage");
    }

    /**
     * Checks if tokens exist in any storage.
     *
     * @return true if tokens are stored
     */
    public boolean hasStoredTokens() {
        return primaryStorage.exists() ||
               (fallbackStorage != primaryStorage && fallbackStorage.exists());
    }

    /**
     * Gets the current storage type being used.
     *
     * @return Storage type name
     */
    public String getStorageType() {
        if (primaryStorage.exists()) {
            return primaryStorage.getStorageType();
        }
        if (fallbackStorage != primaryStorage && fallbackStorage.exists()) {
            return fallbackStorage.getStorageType();
        }
        return primaryStorage.getStorageType();
    }

    private Path resolveStoragePath(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Paths.get(configuredPath);
        }

        // Default to user's data directory
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        Path basePath;
        if (os.contains("win")) {
            // Windows: %APPDATA%\SelfEmployment
            String appData = System.getenv("APPDATA");
            basePath = appData != null
                ? Paths.get(appData, "SelfEmployment")
                : Paths.get(userHome, "AppData", "Roaming", "SelfEmployment");
        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/SelfEmployment
            basePath = Paths.get(userHome, "Library", "Application Support", "SelfEmployment");
        } else {
            // Linux/Unix: ~/.local/share/selfemployment
            String xdgData = System.getenv("XDG_DATA_HOME");
            basePath = xdgData != null
                ? Paths.get(xdgData, "selfemployment")
                : Paths.get(userHome, ".local", "share", "selfemployment");
        }

        return basePath.resolve("hmrc-tokens.enc");
    }
}
