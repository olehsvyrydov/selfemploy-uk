-- SE-SH-002: 6-Year Data Retention Policy
-- HMRC requires tax records to be retained for 6 years after the filing deadline.
-- Filing deadline = 31 January following tax year end
-- Retention period = Filing deadline + 6 years
-- Formula: retention_required_until = CONCAT(tax_year_start + 7, '-01-31')
--
-- Example: Tax Year 2024/25
--   - Tax year ends: 5 April 2025
--   - Filing deadline: 31 January 2026
--   - Retention until: 31 January 2032

-- Add retention_required_until: Date until which the submission must be retained
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS retention_required_until DATE;

-- Add is_deletable: Flag indicating if the submission can be deleted (after retention expires and approval)
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS is_deletable BOOLEAN DEFAULT FALSE;

-- Add deletion_approved_at: Timestamp when deletion was approved
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS deletion_approved_at TIMESTAMP;

-- Add deletion_approved_by: Who approved the deletion (user ID or system)
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS deletion_approved_by VARCHAR(100);

-- Add deletion_reason: Reason for deletion approval
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(255);

-- Create index for retention queries (finding records with expired retention)
CREATE INDEX IF NOT EXISTS idx_submissions_retention ON submissions(retention_required_until);

-- Create index for deletable records
CREATE INDEX IF NOT EXISTS idx_submissions_deletable ON submissions(is_deletable);

-- Backfill existing records with calculated retention dates
-- retention_required_until = January 31st of (tax_year_start + 7)
-- Example: tax_year_start=2024 -> retention_required_until = 2031-01-31
UPDATE submissions
SET retention_required_until = CAST(CONCAT(tax_year_start + 7, '-01-31') AS DATE)
WHERE retention_required_until IS NULL;

-- Comments for documentation
COMMENT ON COLUMN submissions.retention_required_until IS 'Date until which the record must be retained per HMRC 6-year rule';
COMMENT ON COLUMN submissions.is_deletable IS 'True if retention period has expired AND deletion has been approved';
COMMENT ON COLUMN submissions.deletion_approved_at IS 'When deletion was approved (UTC)';
COMMENT ON COLUMN submissions.deletion_approved_by IS 'Who approved the deletion (user email or SYSTEM)';
COMMENT ON COLUMN submissions.deletion_reason IS 'Reason for approving deletion';
