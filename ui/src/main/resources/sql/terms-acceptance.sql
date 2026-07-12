-- SQL for SqliteTermsAcceptanceRepository. Loaded by NamedSql; each block is delimited by
-- a "-- name: <key>" marker. The terms_acceptance table itself is created by SqliteDataStore's
-- schema initialisation.

-- name: insertTermsAcceptance
INSERT INTO terms_acceptance (id, tos_version, accepted_at, scroll_completed_at, application_version)
VALUES (?, ?, ?, ?, ?);

-- name: findLatestTermsVersion
SELECT tos_version FROM terms_acceptance ORDER BY accepted_at DESC LIMIT 1;

-- name: findLatestTermsAcceptedAt
SELECT accepted_at FROM terms_acceptance ORDER BY accepted_at DESC LIMIT 1;

-- name: findLatestTermsScrollCompletedAt
SELECT scroll_completed_at FROM terms_acceptance ORDER BY accepted_at DESC LIMIT 1;
