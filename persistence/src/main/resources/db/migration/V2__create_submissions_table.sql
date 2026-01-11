-- V2: Create submissions table for HMRC MTD quarterly/annual submissions
-- Tracks submission history with status and HMRC reference

CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    tax_year_start INT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_income DECIMAL(12, 2) NOT NULL,
    total_expenses DECIMAL(12, 2) NOT NULL,
    net_profit DECIMAL(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    hmrc_reference VARCHAR(100),
    error_message VARCHAR(2000),
    submitted_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_submission_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT chk_submission_type CHECK (type IN ('QUARTERLY_Q1', 'QUARTERLY_Q2', 'QUARTERLY_Q3', 'QUARTERLY_Q4', 'ANNUAL')),
    CONSTRAINT chk_submission_status CHECK (status IN ('PENDING', 'SUBMITTED', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT chk_submission_dates CHECK (period_end >= period_start)
);

-- Indexes for common queries
CREATE INDEX idx_submissions_business_id ON submissions(business_id);
CREATE INDEX idx_submissions_tax_year ON submissions(tax_year_start);
CREATE INDEX idx_submissions_type_status ON submissions(type, status);

-- Note: Partial unique indexes are not supported in H2 (used for local dev/test)
-- Duplicate submission prevention is enforced at application layer via SubmissionRepository.existsQuarterlySubmission()
