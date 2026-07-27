package uk.selfemploy.ui.controller;

import uk.selfemploy.ui.service.security.AppLockService;

/**
 * An app-lock screen: it works on the key vault and needs to close its own window. Lets a caller load
 * and wire any of them — unlock, enable, change passphrase, regenerate recovery — without knowing which.
 */
public interface AppLockDialog extends DialogStageAware {

    /** Gives the screen the service that owns the key vault. */
    void setAppLockService(AppLockService appLock);
}
