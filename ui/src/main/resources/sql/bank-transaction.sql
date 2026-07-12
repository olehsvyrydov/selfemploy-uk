-- SQL for SqliteBankTransactionRepository. Loaded by NamedSql; each block is delimited by a
-- "-- name: <key>" marker. The bank_transactions and transaction_modification_log table DDL is
-- handled by SqliteDataStore's schema init.

-- name: insertBankTransaction
INSERT OR REPLACE INTO bank_transactions
    (id, business_id, import_audit_id, source_format_id, date, amount,
     description, account_last_four, bank_transaction_id, transaction_hash,
     review_status, income_id, expense_id, exclusion_reason,
     is_business, confidence_score, suggested_category, created_at, updated_at,
     deleted_at, deleted_by, deletion_reason)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- name: findBankTransactionsByBusiness
SELECT * FROM bank_transactions WHERE business_id = ? AND deleted_at IS NULL ORDER BY date DESC;

-- name: findBankTransactionById
SELECT * FROM bank_transactions WHERE id = ? AND deleted_at IS NULL;

-- name: countBankTransactionsByBusinessAndStatus
SELECT COUNT(*) FROM bank_transactions
WHERE business_id = ? AND review_status = ? AND deleted_at IS NULL;

-- name: countBankTransactionsByBusiness
SELECT COUNT(*) FROM bank_transactions WHERE business_id = ? AND deleted_at IS NULL;

-- name: existsByBusinessAndTransactionHash
SELECT COUNT(*) FROM bank_transactions
WHERE business_id = ? AND transaction_hash = ? AND deleted_at IS NULL;

-- name: softDeleteBankTransaction
UPDATE bank_transactions SET deleted_at = ?, deleted_by = ?, deletion_reason = ?
WHERE id = ? AND deleted_at IS NULL;

-- name: insertModificationLog
INSERT INTO transaction_modification_log
    (id, bank_transaction_id, modification_type, field_name,
     previous_value, new_value, modified_by, modified_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

-- name: findModificationLogsByBankTransaction
SELECT * FROM transaction_modification_log
WHERE bank_transaction_id = ? ORDER BY modified_at ASC;
