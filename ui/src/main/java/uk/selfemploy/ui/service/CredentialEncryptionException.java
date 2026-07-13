package uk.selfemploy.ui.service;

/**
 * Thrown when credential encryption or decryption fails.
 */
public class CredentialEncryptionException extends RuntimeException {

    public CredentialEncryptionException(String message) {
        super(message);
    }

    public CredentialEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
