package uk.selfemploy.hmrc.oauth.storage;

/**
 * Exception thrown when token storage operations fail.
 */
public class TokenStorageException extends RuntimeException {

    public enum StorageError {
        ENCRYPTION_FAILED("Failed to encrypt token data"),
        DECRYPTION_FAILED("Failed to decrypt token data"),
        KEYCHAIN_ACCESS_DENIED("Access to system keychain was denied"),
        KEYCHAIN_UNAVAILABLE("System keychain is not available"),
        FILE_ACCESS_ERROR("Failed to access token storage file"),
        SERIALIZATION_ERROR("Failed to serialize/deserialize token data"),
        STORAGE_CORRUPTED("Token storage data is corrupted");

        private final String description;

        StorageError(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final StorageError error;

    public TokenStorageException(StorageError error) {
        super(error.getDescription());
        this.error = error;
    }

    public TokenStorageException(StorageError error, String additionalInfo) {
        super(error.getDescription() + ": " + additionalInfo);
        this.error = error;
    }

    public TokenStorageException(StorageError error, Throwable cause) {
        super(error.getDescription(), cause);
        this.error = error;
    }

    public StorageError getError() {
        return error;
    }
}
