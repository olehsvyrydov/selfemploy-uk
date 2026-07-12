-- SQL for the SqliteMigrationRunner schema_version ledger. Loaded by NamedSql.

-- name: createSchemaVersionTable
CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    applied_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- name: maxSchemaVersion
SELECT COALESCE(MAX(version), 0) FROM schema_version;

-- name: insertSchemaVersion
INSERT INTO schema_version (version, name) VALUES (?, ?);
