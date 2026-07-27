package uk.selfemploy.ui.viewmodel;

/**
 * The rules behind protecting a backup: whether encryption is offered on by default, and what makes a
 * backup passphrase acceptable. Held here, free of JavaFX types, so the decision that governs whether a
 * person's financial records leave the app in the clear is covered by the ordinary test run.
 */
public class BackupExportViewModel {

    /**
     * Whether the encrypt option starts ticked.
     *
     * <p>If the database is encrypted, a plaintext backup hands away exactly what that protects, so the
     * safe answer is the automatic one. If the user has chosen not to protect the database, encrypting
     * the backup would guard a copy more carefully than the original — offered, but not assumed.
     *
     * @param protectionEnabled whether a passphrase vault exists
     */
    public boolean encryptByDefault(boolean protectionEnabled) {
        return protectionEnabled;
    }

    /** The outcome of checking a backup passphrase, carrying the message that describes it. */
    public enum Validation {
        OK(null),
        TOO_SHORT("backup.error.tooShort", AppProtectViewModel.MIN_PASSPHRASE_LENGTH),
        MISMATCH("backup.error.mismatch");

        private final String messageKey;
        private final Object[] messageArgs;

        Validation(String messageKey, Object... messageArgs) {
            this.messageKey = messageKey;
            this.messageArgs = messageArgs;
        }

        public String messageKey() {
            return messageKey;
        }

        public Object[] messageArgs() {
            return messageArgs.clone();
        }
    }

    /**
     * Checks the passphrase chosen for a backup.
     *
     * <p>Held to the same length as the app passphrase: a backup file can be carried off and attacked at
     * leisure, so it faces the same offline attack the vault does, and deserves the same floor.
     *
     * @param passphrase   the chosen passphrase, never null
     * @param confirmation the repeated passphrase, ignored when {@code reusingAppPassphrase} is set,
     *                     because that one is checked against the vault rather than retyped
     */
    public Validation validate(String passphrase, String confirmation, boolean reusingAppPassphrase) {
        if (passphrase.length() < AppProtectViewModel.MIN_PASSPHRASE_LENGTH) {
            return Validation.TOO_SHORT;
        }
        if (!reusingAppPassphrase && !passphrase.equals(confirmation)) {
            return Validation.MISMATCH;
        }
        return Validation.OK;
    }

    /**
     * Whether the export may go ahead.
     *
     * <p>An unencrypted backup is a deliberate choice and always allowed — a readable file is what a
     * user hands their accountant. Encryption, once chosen, has to be complete.
     */
    public boolean canExport(boolean encrypt, Validation validation) {
        return !encrypt || validation == Validation.OK;
    }
}
