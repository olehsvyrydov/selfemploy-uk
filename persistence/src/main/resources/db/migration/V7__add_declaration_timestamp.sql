-- SE-803: Declaration Timestamp Persistence
-- Migration to add declaration acceptance fields to submissions table for audit trail

-- Add declaration acceptance timestamp (UTC)
-- This stores when the user accepted the HMRC declaration
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS declaration_accepted_at TIMESTAMP;

-- Add declaration text hash for version tracking
-- SHA-256 hash (64 hex characters) allows detecting if declaration text changed
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS declaration_text_hash VARCHAR(64);

-- Index for audit queries (finding submissions by declaration timestamp)
CREATE INDEX IF NOT EXISTS idx_submissions_declaration_at ON submissions(declaration_accepted_at);
