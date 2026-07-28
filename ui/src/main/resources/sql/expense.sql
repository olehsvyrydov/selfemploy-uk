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

-- name: selectExpenseAmountsByBusinessAndDateRange
-- Amounts, not a SUM: SQLite has no decimal type, so SUM() over these returns a float and
-- 10.10 three times over comes to 30.299999999999997. They are added up in BigDecimal instead.
SELECT amount FROM expenses
WHERE business_id = ? AND date >= ? AND date <= ?;

-- name: selectAllowableExpenseAmountsByBusinessAndDateRange
SELECT amount FROM expenses
WHERE business_id = ? AND date >= ? AND date <= ? AND category IN (%s);

-- name: deleteExpenseById
DELETE FROM expenses WHERE id = ?;

-- name: countExpensesByBusiness
SELECT COUNT(*) FROM expenses WHERE business_id = ?;
