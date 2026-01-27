-- SE-SH-001: Add UTR and NINO columns to submissions table
-- These fields enable linking submissions to the taxpayer's identifiers
-- for regulatory compliance and audit purposes.
--
-- UTR (Unique Taxpayer Reference): 10 digits, identifies the business with HMRC
-- NINO (National Insurance Number): 2 letters + 6 digits + 1 letter suffix

-- Add UTR column (Unique Taxpayer Reference)
-- Nullable as existing submissions may not have UTR populated
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS utr VARCHAR(10);

-- Add NINO column (National Insurance Number)
-- Nullable as existing submissions may not have NINO populated
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS nino VARCHAR(9);

-- Create index for UTR queries (e.g., finding all submissions for a UTR)
CREATE INDEX IF NOT EXISTS idx_submissions_utr ON submissions(utr);

-- Create index for NINO queries (e.g., finding all submissions for a NINO)
CREATE INDEX IF NOT EXISTS idx_submissions_nino ON submissions(nino);

-- Note: Application layer enforces validation rules:
-- UTR: Exactly 10 digits (^\d{10}$)
-- NINO: ^(?!BG|GB|KN|NK|NT|TN|ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z]\d{6}[A-D]$
-- Both stored in UPPERCASE
