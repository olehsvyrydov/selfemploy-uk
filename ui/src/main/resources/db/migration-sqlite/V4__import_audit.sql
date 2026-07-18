-- Per-import audit trail for the desktop (SQLite) runtime.
-- Mirrors the persistence-module import_audit table (V10) so bank imports have a
-- first-class history record and can be undone. bank_transactions.import_audit_id
-- already references an import's id; this table gives that id a home.
CREATE TABLE IF NOT EXISTS import_audit (
    id                TEXT PRIMARY KEY,
    business_id       TEXT NOT NULL,
    import_timestamp  TEXT NOT NULL,
    file_name         TEXT,
    file_hash         TEXT,
    import_type       TEXT NOT NULL,
    total_records     INTEGER NOT NULL DEFAULT 0,
    imported_count    INTEGER NOT NULL DEFAULT 0,
    skipped_count     INTEGER NOT NULL DEFAULT 0,
    record_ids        TEXT,                      -- comma-separated staged transaction ids (for undo)
    status            TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'UNDONE')),
    undone_at         TEXT,
    undone_by         TEXT
);

CREATE INDEX IF NOT EXISTS idx_import_audit_business ON import_audit (business_id);
CREATE INDEX IF NOT EXISTS idx_import_audit_timestamp ON import_audit (import_timestamp);
