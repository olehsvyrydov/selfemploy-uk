-- Links approved bank transactions to their final Income/Expense records.
-- Part of the digital link chain: ImportAudit → BankTransaction → Income/Expense

ALTER TABLE incomes ADD COLUMN bank_transaction_id UUID;
ALTER TABLE expenses ADD COLUMN bank_transaction_id UUID;

ALTER TABLE incomes ADD CONSTRAINT fk_income_bank_tx
    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id);

ALTER TABLE expenses ADD CONSTRAINT fk_expense_bank_tx
    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id);

CREATE INDEX idx_income_bank_tx ON incomes(bank_transaction_id);
CREATE INDEX idx_expense_bank_tx ON expenses(bank_transaction_id);
