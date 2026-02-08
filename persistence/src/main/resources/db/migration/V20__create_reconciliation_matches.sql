-- Reconciliation matches table for tracking duplicate detection between
-- bank-imported transactions and manually entered income/expense records.
-- Required for audit trail compliance (TMA 1970 s.12B).
-- Records are statutory and must never be hard-deleted.

CREATE TABLE IF NOT EXISTS reconciliation_matches (
    id VARCHAR(36) PRIMARY KEY,
    bank_transaction_id VARCHAR(36) NOT NULL,
    manual_transaction_id VARCHAR(36) NOT NULL,
    manual_transaction_type VARCHAR(10) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    match_tier VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNRESOLVED',
    business_id VARCHAR(36) NOT NULL,
    created_at VARCHAR(50) NOT NULL,
    resolved_at VARCHAR(50),
    resolved_by VARCHAR(100),
    UNIQUE(bank_transaction_id, manual_transaction_id, manual_transaction_type)
);

CREATE INDEX IF NOT EXISTS idx_recon_business ON reconciliation_matches(business_id);
CREATE INDEX IF NOT EXISTS idx_recon_status ON reconciliation_matches(business_id, status);
CREATE INDEX IF NOT EXISTS idx_recon_bank_tx ON reconciliation_matches(bank_transaction_id);
