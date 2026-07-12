-- SQL for SqlitePrivacyAcknowledgmentRepository. Loaded by NamedSql; each block is delimited
-- by a "-- name: <key>" marker. The privacy_acknowledgment table is created by SqliteDataStore's
-- schema initialisation.

-- name: insertPrivacyAcknowledgment
INSERT INTO privacy_acknowledgment (id, privacy_version, acknowledged_at, application_version)
VALUES (?, ?, ?, ?);

-- name: findLatestPrivacyVersion
SELECT privacy_version FROM privacy_acknowledgment ORDER BY acknowledged_at DESC LIMIT 1;

-- name: findLatestPrivacyAcknowledgedAt
SELECT acknowledged_at FROM privacy_acknowledgment ORDER BY acknowledged_at DESC LIMIT 1;
