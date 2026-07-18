-- Persisted read/snooze state for deadline reminders, keyed by a restart-stable reminder key
-- (deadline label + date + trigger offset). Reminders themselves are regenerated from deadlines
-- on each launch; this table lets a dismissed or snoozed reminder stay dismissed/snoozed.
CREATE TABLE IF NOT EXISTS notification_state (
    state_key     TEXT PRIMARY KEY,
    is_read       INTEGER NOT NULL DEFAULT 0,
    snooze_until  TEXT,
    updated_at    TEXT NOT NULL
);
