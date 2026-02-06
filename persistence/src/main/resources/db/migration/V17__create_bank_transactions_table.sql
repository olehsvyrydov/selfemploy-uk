-- Bank transaction staging table for import review workflow.
-- Imported transactions are staged here before being promoted to incomes/expenses.
-- State machine: PENDING → CATEGORIZED | EXCLUDED | SKIPPED

CREATE TABLE bank_transactions (
    id              UUID PRIMARY KEY,
    business_id     UUID NOT NULL,
    import_audit_id UUID NOT NULL,
    source_format_id VARCHAR(50),          -- nullable: NULL = unknown format, e.g. "csv-barclays"

    -- Transaction data (minimal fields per GDPR data minimization)
    date            DATE NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    description     VARCHAR(500) NOT NULL,
    account_last_four VARCHAR(4),
    bank_transaction_id VARCHAR(100),

    -- Duplicate detection via SHA-256 hash of date+amount+description+accountLastFour
    transaction_hash VARCHAR(64) NOT NULL,

    -- Review workflow
    review_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    income_id       UUID,
    expense_id      UUID,
    exclusion_reason VARCHAR(200),

    -- Business vs personal flag (nullable = uncategorized)
    is_business     BOOLEAN,

    -- Auto-categorization suggestion with confidence score
    confidence_score DECIMAL(5, 4),
    suggested_category VARCHAR(50),

    -- Timestamps
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,

    -- Soft delete
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(100),
    deletion_reason VARCHAR(500),

    -- Foreign keys
    CONSTRAINT fk_bank_tx_business FOREIGN KEY (business_id) REFERENCES businesses(id),
    CONSTRAINT fk_bank_tx_import_audit FOREIGN KEY (import_audit_id) REFERENCES import_audit(id),
    CONSTRAINT fk_bank_tx_income FOREIGN KEY (income_id) REFERENCES incomes(id),
    CONSTRAINT fk_bank_tx_expense FOREIGN KEY (expense_id) REFERENCES expenses(id)
);

-- Performance indexes
CREATE INDEX idx_bank_tx_business_id ON bank_transactions(business_id);
CREATE INDEX idx_bank_tx_import_audit_id ON bank_transactions(import_audit_id);
CREATE INDEX idx_bank_tx_hash ON bank_transactions(transaction_hash);
CREATE INDEX idx_bank_tx_review_status ON bank_transactions(business_id, review_status);
CREATE INDEX idx_bank_tx_date ON bank_transactions(business_id, date);
CREATE INDEX idx_bank_tx_deleted ON bank_transactions(business_id, deleted_at);
