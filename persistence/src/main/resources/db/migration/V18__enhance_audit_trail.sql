-- Enhanced audit trail for digital link chain and MTD compliance.
-- Adds original file tracking, retention policy, and user identity to import_audit.
-- Creates transaction_modification_log for tracking all changes to bank transactions.

-- Import audit enhancements: original file storage, retention, and user tracking
ALTER TABLE import_audit ADD COLUMN original_file_path VARCHAR(500);
ALTER TABLE import_audit ADD COLUMN original_file_encrypted BOOLEAN DEFAULT true;
ALTER TABLE import_audit ADD COLUMN retention_until DATE;
ALTER TABLE import_audit ADD COLUMN imported_by VARCHAR(255);

-- Transaction modification log: immutable audit trail for all bank transaction changes
CREATE TABLE transaction_modification_log (
    id                   UUID PRIMARY KEY,
    bank_transaction_id  UUID NOT NULL,
    modification_type    VARCHAR(50) NOT NULL,
    field_name           VARCHAR(50),
    previous_value       TEXT,
    new_value            TEXT,
    modified_by          VARCHAR(255) NOT NULL,
    modified_at          TIMESTAMP NOT NULL,

    CONSTRAINT fk_mod_log_bank_tx FOREIGN KEY (bank_transaction_id)
        REFERENCES bank_transactions(id),

    CONSTRAINT chk_mod_type CHECK (
        modification_type IN (
            'CATEGORIZED', 'EXCLUDED', 'RECATEGORIZED',
            'RESTORED', 'BUSINESS_PERSONAL_CHANGED', 'CATEGORY_CHANGED'
        )
    )
);

-- Performance indexes for modification log
CREATE INDEX idx_mod_log_bank_tx ON transaction_modification_log(bank_transaction_id);
CREATE INDEX idx_mod_log_time ON transaction_modification_log(modified_at);

-- Index for retention policy queries
CREATE INDEX idx_import_audit_retention ON import_audit(retention_until);
