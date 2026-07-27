package uk.selfemploy.ui.viewmodel;

/**
 * Decision logic behind the Settings "Security" section: what the app may honestly claim about the
 * protection of the data on disk, and which controls that state allows. Held here rather than in the
 * controller so it can be tested without a JavaFX toolkit.
 */
public class SecuritySettingsViewModel {

    /**
     * What is actually true of the data on disk right now.
     *
     * <p>Enabling protection writes the key vault but defers the one-time encryption to the next launch
     * (the database is open and in use when the user enables it). Between those two moments the vault
     * exists while the file is still plaintext, and the app must say so rather than claim protection it
     * does not yet have.
     */
    public enum ProtectionStatus {
        OFF("settings.security.status.off", "settings.security.status.off.description"),
        PENDING_RESTART("settings.security.status.pending", "settings.security.status.pending.description"),
        ON("settings.security.status.on", "settings.security.status.on.description");

        private final String labelKey;
        private final String descriptionKey;

        ProtectionStatus(String labelKey, String descriptionKey) {
            this.labelKey = labelKey;
            this.descriptionKey = descriptionKey;
        }

        public String labelKey() {
            return labelKey;
        }

        public String descriptionKey() {
            return descriptionKey;
        }
    }

    /**
     * The true protection state.
     *
     * @param vaultExists        whether a key vault has been written (the user enabled protection)
     * @param databaseIsPlaintext whether the database file is still readable without a key
     */
    public ProtectionStatus status(boolean vaultExists, boolean databaseIsPlaintext) {
        if (!vaultExists) {
            return ProtectionStatus.OFF;
        }
        return databaseIsPlaintext ? ProtectionStatus.PENDING_RESTART : ProtectionStatus.ON;
    }

    /** Protection can only be turned on when it is off; there is no supported way to turn it back off. */
    public boolean canEnableProtection(ProtectionStatus status) {
        return status == ProtectionStatus.OFF;
    }

    /**
     * Whether the passphrase and recovery-code controls apply. They act on the key vault, which exists
     * as soon as protection is enabled, so they work before the database has actually been encrypted.
     */
    public boolean canManageKeys(ProtectionStatus status) {
        return status != ProtectionStatus.OFF;
    }
}
