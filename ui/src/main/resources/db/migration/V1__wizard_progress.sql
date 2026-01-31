-- Flyway Migration: wizard_progress table
-- SE-12-006: Wizard State Persistence
--
-- Stores wizard progress for resuming across sessions.
-- NINO is stored encrypted (AES-256-GCM) for security.

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

-- Create index on wizard_type for faster lookups
CREATE INDEX IF NOT EXISTS idx_wizard_progress_type ON wizard_progress(wizard_type);
