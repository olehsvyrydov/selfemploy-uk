-- V12__add_unique_identifier_fields.sql
-- Sprint 10C: Enhanced Duplicate Detection - SE-10C-002
-- Adds unique identifier fields for more accurate duplicate detection

-- Income table enhancements
ALTER TABLE incomes ADD COLUMN bank_transaction_ref VARCHAR(100);
ALTER TABLE incomes ADD COLUMN invoice_number VARCHAR(50);
ALTER TABLE incomes ADD COLUMN receipt_path VARCHAR(500);

-- Expense table enhancements
ALTER TABLE expenses ADD COLUMN bank_transaction_ref VARCHAR(100);
ALTER TABLE expenses ADD COLUMN supplier_ref VARCHAR(100);
ALTER TABLE expenses ADD COLUMN invoice_number VARCHAR(50);

-- Indexes for duplicate detection (H2 compatible - no partial indexes)
-- Composite indexes for efficient lookup by business + reference field
CREATE INDEX idx_incomes_bank_ref ON incomes(business_id, bank_transaction_ref);
CREATE INDEX idx_incomes_invoice ON incomes(business_id, invoice_number);
CREATE INDEX idx_expenses_bank_ref ON expenses(business_id, bank_transaction_ref);
CREATE INDEX idx_expenses_supplier ON expenses(business_id, supplier_ref);
CREATE INDEX idx_expenses_invoice ON expenses(business_id, invoice_number);

-- Note: H2 does not support partial indexes with WHERE clause.
-- Unique constraint for bank_transaction_ref is enforced at application level
-- via repository existsByBusinessIdAndBankTransactionRef() checks.
