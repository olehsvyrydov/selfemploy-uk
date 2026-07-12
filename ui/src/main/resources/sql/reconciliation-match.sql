-- SQL for SqliteReconciliationMatchRepository. Loaded by NamedSql; each block is delimited by a
-- "-- name: <key>" marker. The reconciliation_matches table DDL is handled by SqliteDataStore's
-- schema init.

-- name: insertReconciliationMatch
INSERT OR REPLACE INTO reconciliation_matches
    (id, bank_transaction_id, manual_transaction_id, manual_transaction_type,
     confidence, match_tier, status, business_id, created_at, resolved_at, resolved_by)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- name: findReconciliationMatchById
SELECT * FROM reconciliation_matches WHERE id = ?;

-- name: findReconciliationMatchesByBankTransaction
SELECT * FROM reconciliation_matches WHERE bank_transaction_id = ? ORDER BY confidence DESC;

-- name: findReconciliationMatchesByBusiness
SELECT * FROM reconciliation_matches WHERE business_id = ? ORDER BY created_at DESC;

-- name: findUnresolvedReconciliationMatchesByBusiness
SELECT * FROM reconciliation_matches
WHERE business_id = ? AND status = 'UNRESOLVED' ORDER BY confidence DESC;

-- name: countUnresolvedReconciliationMatchesByBusiness
SELECT COUNT(*) FROM reconciliation_matches WHERE business_id = ? AND status = 'UNRESOLVED';

-- name: updateReconciliationMatchStatus
UPDATE reconciliation_matches SET status = ?, resolved_at = ?, resolved_by = ? WHERE id = ?;
