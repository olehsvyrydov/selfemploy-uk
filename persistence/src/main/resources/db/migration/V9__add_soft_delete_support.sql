-- V9: Add soft delete support for income and expense records
-- Supports import undo/rollback feature (Sprint 10B - SE-10B-003)

-- Add soft delete columns to incomes table
ALTER TABLE incomes ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE incomes ADD COLUMN deleted_by VARCHAR(255);
ALTER TABLE incomes ADD COLUMN deletion_reason VARCHAR(255);

-- Add soft delete columns to expenses table
ALTER TABLE expenses ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE expenses ADD COLUMN deleted_by VARCHAR(255);
ALTER TABLE expenses ADD COLUMN deletion_reason VARCHAR(255);

-- Create indexes for efficient filtering of active records
-- Note: H2 does not support partial indexes, so we use standard indexes
CREATE INDEX idx_incomes_deleted_at ON incomes(deleted_at);
CREATE INDEX idx_expenses_deleted_at ON expenses(deleted_at);
