-- SQL for SqliteSubmissionRepository. Loaded by NamedSql; each block is delimited by a
-- "-- name: <key>" marker. The submissions table (and the honesty migration) are handled by
-- SqliteDataStore's schema initialisation.

-- name: insertSubmission
INSERT OR REPLACE INTO submissions
    (id, business_id, type, tax_year_start, period_start, period_end,
     total_income, total_expenses, net_profit, status, hmrc_reference,
     error_message, submitted_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- name: findSubmissionsByBusiness
SELECT * FROM submissions WHERE business_id = ? ORDER BY submitted_at DESC;

-- name: findSubmissionById
SELECT * FROM submissions WHERE id = ?;

-- name: findSubmissionsByBusinessAndTaxYear
SELECT * FROM submissions WHERE business_id = ? AND tax_year_start = ? ORDER BY submitted_at DESC;

-- name: deleteSubmissionById
DELETE FROM submissions WHERE id = ?;

-- name: countSubmissionsByBusiness
SELECT COUNT(*) FROM submissions WHERE business_id = ?;
