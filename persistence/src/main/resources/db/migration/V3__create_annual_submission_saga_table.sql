-- Create annual_submission_sagas table for saga state persistence
CREATE TABLE annual_submission_sagas (
    id UUID PRIMARY KEY,
    nino VARCHAR(9) NOT NULL,
    tax_year_start INTEGER NOT NULL,
    state VARCHAR(20) NOT NULL,
    calculation_id VARCHAR(100),
    total_income DECIMAL(12, 2),
    total_expenses DECIMAL(12, 2),
    net_profit DECIMAL(12, 2),
    income_tax DECIMAL(12, 2),
    ni_class2 DECIMAL(12, 2),
    ni_class4 DECIMAL(12, 2),
    total_tax_liability DECIMAL(12, 2),
    hmrc_confirmation VARCHAR(100),
    error_message VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Index for finding saga by NINO and tax year (unique constraint)
CREATE UNIQUE INDEX idx_annual_saga_nino_tax_year ON annual_submission_sagas(nino, tax_year_start);

-- Index for filtering by state
CREATE INDEX idx_annual_saga_state ON annual_submission_sagas(state);

-- Comments for documentation
COMMENT ON TABLE annual_submission_sagas IS 'Stores state for Annual Self Assessment submission sagas to enable resume capability';
COMMENT ON COLUMN annual_submission_sagas.state IS 'Current state: INITIATED, CALCULATING, CALCULATED, DECLARING, COMPLETED, FAILED';
COMMENT ON COLUMN annual_submission_sagas.calculation_id IS 'HMRC calculation ID returned from trigger calculation API';
COMMENT ON COLUMN annual_submission_sagas.hmrc_confirmation IS 'HMRC charge reference from final declaration submission';
