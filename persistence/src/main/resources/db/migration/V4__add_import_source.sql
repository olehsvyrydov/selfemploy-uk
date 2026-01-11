-- V4: Add source tracking for bank imports
-- SE-601: CSV Bank Import feature

-- Add source column to incomes table (default to MANUAL for existing records)
ALTER TABLE incomes ADD COLUMN source VARCHAR(20) DEFAULT 'MANUAL' NOT NULL;
ALTER TABLE incomes ADD COLUMN import_batch_id UUID;

-- Add source column to expenses table (default to MANUAL for existing records)
ALTER TABLE expenses ADD COLUMN source VARCHAR(20) DEFAULT 'MANUAL' NOT NULL;
ALTER TABLE expenses ADD COLUMN import_batch_id UUID;

-- Create import_batches table to track import history
CREATE TABLE import_batches (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    bank_name VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    total_transactions INTEGER NOT NULL,
    income_count INTEGER NOT NULL,
    expense_count INTEGER NOT NULL,
    duplicate_count INTEGER NOT NULL,
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_batch_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

-- Add foreign key constraints for import_batch_id
ALTER TABLE incomes ADD CONSTRAINT fk_income_import_batch
    FOREIGN KEY (import_batch_id) REFERENCES import_batches(id) ON DELETE SET NULL;
ALTER TABLE expenses ADD CONSTRAINT fk_expense_import_batch
    FOREIGN KEY (import_batch_id) REFERENCES import_batches(id) ON DELETE SET NULL;

-- Indexes for efficient queries
CREATE INDEX idx_incomes_source ON incomes(source);
CREATE INDEX idx_incomes_import_batch ON incomes(import_batch_id);
CREATE INDEX idx_expenses_source ON expenses(source);
CREATE INDEX idx_expenses_import_batch ON expenses(import_batch_id);
CREATE INDEX idx_import_batches_business ON import_batches(business_id);
CREATE INDEX idx_import_batches_imported_at ON import_batches(imported_at);
