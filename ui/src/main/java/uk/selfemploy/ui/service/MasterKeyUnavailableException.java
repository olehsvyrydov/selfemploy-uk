package uk.selfemploy.ui.service;

/**
 * Thrown when the master key cannot be read or created — a damaged, wrong-length, or momentarily
 * unreadable key file, or a home directory that cannot be written.
 *
 * <p>This is distinct from a plain {@link CredentialEncryptionException}, which signals that a
 * value failed to decrypt under an <em>available</em> key (i.e. genuinely corrupt ciphertext). The
 * distinction matters at the storage layer: a value that cannot be read because the key is
 * temporarily unavailable is still recoverable once the key is restored, so callers must not delete
 * it, whereas corrupt ciphertext is safe to clear.</p>
 */
public class MasterKeyUnavailableException extends CredentialEncryptionException {

    public MasterKeyUnavailableException(String message) {
        super(message);
    }

    public MasterKeyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
