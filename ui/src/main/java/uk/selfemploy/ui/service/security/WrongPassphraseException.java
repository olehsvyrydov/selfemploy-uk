package uk.selfemploy.ui.service.security;

/** Thrown when a passphrase or recovery code fails to unwrap the database key (wrong secret). */
public class WrongPassphraseException extends Exception {
    public WrongPassphraseException() {
        super("The passphrase or recovery code is incorrect.");
    }
}
