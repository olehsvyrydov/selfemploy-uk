package uk.selfemploy.ui.service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Persists the read/snooze state of deadline reminders keyed by their restart-stable identity
 * ({@link DeadlineNotification#stableKey()}), so a dismissed or snoozed reminder is not shown again
 * after the app is relaunched and the reminder is regenerated from the deadline.
 */
public interface NotificationStateStore {

    /** A persisted reminder's read flag and snooze deadline (snoozeUntil may be null). */
    record PersistedState(boolean read, LocalDateTime snoozeUntil) {
    }

    /** Records the current read/snooze state for a reminder key. */
    void save(String key, boolean read, LocalDateTime snoozeUntil);

    /** Loads all persisted reminder states, keyed by reminder key. */
    Map<String, PersistedState> loadAll();

    /** A store that persists nothing — the default until a real store is wired in. */
    NotificationStateStore NOOP = new NotificationStateStore() {
        @Override
        public void save(String key, boolean read, LocalDateTime snoozeUntil) {
            // no-op
        }

        @Override
        public Map<String, PersistedState> loadAll() {
            return Map.of();
        }
    };
}
