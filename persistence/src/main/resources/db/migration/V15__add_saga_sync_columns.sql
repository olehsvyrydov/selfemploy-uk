-- SE-SH-003: Add columns to sync annual saga completion to submissions table
-- When an AnnualSubmissionSaga reaches COMPLETED state, we create/update
-- a corresponding Submission record with calculation details.

-- Add calculation_id: Links submission to HMRC calculation
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS calculation_id VARCHAR(100);

-- Add charge_reference: HMRC charge reference from final declaration
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS charge_reference VARCHAR(100);

-- Add income_tax: Calculated income tax liability
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS income_tax DECIMAL(12, 2);

-- Add ni_class4: National Insurance Class 4 liability
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS ni_class4 DECIMAL(12, 2);

-- Add total_tax_liability: Total tax + NI payable
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS total_tax_liability DECIMAL(12, 2);

-- Add saga_id: Links submission to the saga that created it (for idempotency)
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS saga_id UUID;

-- Create index for saga_id (for idempotency checks)
CREATE INDEX IF NOT EXISTS idx_submissions_saga_id ON submissions(saga_id);

-- Create index for calculation_id (for HMRC reference lookups)
CREATE INDEX IF NOT EXISTS idx_submissions_calculation_id ON submissions(calculation_id);

-- Comments for documentation
COMMENT ON COLUMN submissions.calculation_id IS 'HMRC calculation ID returned from trigger calculation API';
COMMENT ON COLUMN submissions.charge_reference IS 'HMRC charge reference from final declaration submission';
COMMENT ON COLUMN submissions.income_tax IS 'Calculated income tax liability from HMRC';
COMMENT ON COLUMN submissions.ni_class4 IS 'National Insurance Class 4 liability';
COMMENT ON COLUMN submissions.total_tax_liability IS 'Total tax + NI payable to HMRC';
COMMENT ON COLUMN submissions.saga_id IS 'Links to annual_submission_sagas.id for idempotency';
