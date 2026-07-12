-- SQL for SqliteExpenseRepository. Loaded by NamedSql; each block is delimited by a
-- "-- name: <key>" marker. The expenses table DDL is handled by SqliteDataStore's schema init.

-- name: insertExpense
INSERT OR REPLACE INTO expenses
    (id, business_id, date, amount, description, category, receipt_path, notes)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

-- name: findExpenseById
SELECT * FROM expenses WHERE id = ?;

-- name: findExpensesByBusiness
SELECT * FROM expenses WHERE business_id = ? ORDER BY date DESC;

-- name: findExpensesByBusinessAndDateRange
SELECT * FROM expenses WHERE business_id = ? AND date >= ? AND date <= ? ORDER BY date DESC;

-- name: sumExpensesByBusinessAndDateRange
SELECT COALESCE(SUM(CAST(amount AS DECIMAL)), 0) FROM expenses
WHERE business_id = ? AND date >= ? AND date <= ?;

-- name: sumAllowableExpensesByBusinessAndDateRange
SELECT COALESCE(SUM(CAST(amount AS DECIMAL)), 0) FROM expenses
WHERE business_id = ? AND date >= ? AND date <= ? AND category IN (%s);

-- name: deleteExpenseById
DELETE FROM expenses WHERE id = ?;
