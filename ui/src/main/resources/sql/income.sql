-- SQL for SqliteIncomeRepository. Loaded by NamedSql; each block is delimited by a
-- "-- name: <key>" marker. The income table DDL is handled by SqliteDataStore's schema init.

-- name: insertIncome
INSERT OR REPLACE INTO income
    (id, business_id, date, amount, description, category, reference, client_name, status)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

-- name: findIncomeById
SELECT * FROM income WHERE id = ?;

-- name: findIncomeByBusiness
SELECT * FROM income WHERE business_id = ? ORDER BY date DESC;

-- name: findIncomeByBusinessAndDateRange
SELECT * FROM income WHERE business_id = ? AND date >= ? AND date <= ? ORDER BY date DESC;

-- name: sumIncomeByBusinessAndDateRange
SELECT COALESCE(SUM(CAST(amount AS DECIMAL)), 0) FROM income
WHERE business_id = ? AND date >= ? AND date <= ?;

-- name: deleteIncomeById
DELETE FROM income WHERE id = ?;

-- name: countIncomeByBusiness
SELECT COUNT(*) FROM income WHERE business_id = ?;
