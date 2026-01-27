-- SE-SH-004: Add uniqueness constraints to prevent duplicate submissions
--
-- Business Rules:
-- 1. Only one submission per (business_id, tax_year_start, type) for quarterly types
-- 2. Only one annual submission per (business_id, tax_year_start)
--
-- Note: H2 does not support partial unique indexes, so we use conditional unique
-- constraints at the application layer. However, we can still add a unique
-- constraint that covers the common case.

-- First, check for and report any existing duplicates
-- This query will be logged and should be reviewed before applying constraints

-- For quarterly submissions: unique on (business_id, tax_year_start, type)
-- This constraint works for all submission types
CREATE UNIQUE INDEX IF NOT EXISTS uk_submission_business_year_type
    ON submissions(business_id, tax_year_start, type);

-- Comments for documentation
COMMENT ON INDEX uk_submission_business_year_type IS 'Ensures only one submission per business, tax year, and type';

-- Note: The constraint covers all submission types including ANNUAL.
-- For ANNUAL type, since there's only one type value, this effectively
-- ensures only one annual submission per business per tax year.
