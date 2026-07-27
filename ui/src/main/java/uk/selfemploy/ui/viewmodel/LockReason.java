package uk.selfemploy.ui.viewmodel;

/**
 * Why a session locked, so the unlock screen can say so. A user who locked deliberately should not be
 * told the app timed out, and someone returning to a machine that locked itself should not be left
 * wondering whether it restarted.
 */
public enum LockReason {

    /** The user chose to lock. */
    MANUAL("unlock.relocked.manual"),

    /** No activity for the configured timeout. */
    IDLE("unlock.relocked.idle"),

    /** The machine was suspended and has just resumed. */
    SUSPEND("unlock.relocked.suspend");

    private final String messageKey;

    LockReason(String messageKey) {
        this.messageKey = messageKey;
    }

    /** The i18n key for the subtitle explaining this lock. */
    public String messageKey() {
        return messageKey;
    }
}
