-- V11: Add indexes for efficient duplicate detection
-- Supports enhanced duplicate detection feature (Sprint 10B - SE-10B-001)

-- Composite indexes for duplicate detection queries
-- Covers: business_id, transaction_date, amount, and deleted_at for soft delete filtering
CREATE INDEX idx_incomes_dup_detection ON incomes(business_id, date, amount, deleted_at);
CREATE INDEX idx_expenses_dup_detection ON expenses(business_id, date, amount, deleted_at);
