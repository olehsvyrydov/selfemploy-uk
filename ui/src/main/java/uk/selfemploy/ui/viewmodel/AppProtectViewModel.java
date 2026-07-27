package uk.selfemploy.ui.viewmodel;

/**
 * Decision logic behind the optional "protect your data" step: what makes a passphrase acceptable, and
 * when the key vault may be written. Held here rather than in the controller so the rules guarding an
 * irreversible action can be tested without a JavaFX toolkit.
 */
public class AppProtectViewModel {

    /** Shortest passphrase the setup step accepts. */
    public static final int MIN_PASSPHRASE_LENGTH = 8;

    /** The outcome of checking the passphrase pair, carrying the message that describes it. */
    public enum Validation {
        OK(null),
        TOO_SHORT("protect.error.tooShort", MIN_PASSPHRASE_LENGTH),
        MISMATCH("protect.error.mismatch");

        private final String messageKey;
        private final Object[] messageArgs;

        Validation(String messageKey, Object... messageArgs) {
            this.messageKey = messageKey;
            this.messageArgs = messageArgs;
        }

        /** The i18n key for this outcome, or {@code null} when the passphrase is acceptable. */
        public String messageKey() {
            return messageKey;
        }

        /** Arguments the message formats in; empty when the message has no placeholders. */
        public Object[] messageArgs() {
            return messageArgs.clone();
        }
    }

    /**
     * Checks the chosen passphrase and its confirmation. Length is checked first so a user who mistypes
     * a too-short passphrase twice is told the real problem rather than being sent to fix a mismatch.
     *
     * @param passphrase   the chosen passphrase, never null
     * @param confirmation the repeated passphrase, never null
     */
    public Validation validate(String passphrase, String confirmation) {
        if (passphrase.length() < MIN_PASSPHRASE_LENGTH) {
            return Validation.TOO_SHORT;
        }
        if (!passphrase.equals(confirmation)) {
            return Validation.MISMATCH;
        }
        return Validation.OK;
    }

    /**
     * Whether the vault may be written. Protection is irreversible without the passphrase or the
     * recovery code, so the vault is only committed once a code has actually been generated and the
     * user has confirmed they wrote it down.
     *
     * @param recoveryCodeShown whether a recovery code was generated and displayed
     * @param acknowledged      whether the user ticked the "I have written it down" confirmation
     */
    public boolean canCommit(boolean recoveryCodeShown, boolean acknowledged) {
        return recoveryCodeShown && acknowledged;
    }
}
