-- SQL for SqliteWizardProgressRepository. Kept out of the Java code so statements are
-- readable and maintainable in one place. Loaded by NamedSql; each block is delimited by
-- a "-- name: <key>" marker.

-- name: createTable
CREATE TABLE IF NOT EXISTS wizard_progress (
    id INTEGER PRIMARY KEY,
    wizard_type TEXT NOT NULL,
    current_step INTEGER NOT NULL,
    checklist_state TEXT,
    nino_entered TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE(wizard_type)
);

-- name: findByType
SELECT * FROM wizard_progress WHERE wizard_type = ?;

-- name: upsert
INSERT INTO wizard_progress
    (wizard_type, current_step, checklist_state, nino_entered, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?)
ON CONFLICT(wizard_type) DO UPDATE SET
    current_step = excluded.current_step,
    checklist_state = excluded.checklist_state,
    nino_entered = excluded.nino_entered,
    updated_at = excluded.updated_at;

-- name: deleteByType
DELETE FROM wizard_progress WHERE wizard_type = ?;

-- name: findRawNino
SELECT nino_entered FROM wizard_progress WHERE wizard_type = ?;
