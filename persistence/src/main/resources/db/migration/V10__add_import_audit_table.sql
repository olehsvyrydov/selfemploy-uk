-- V10: Create import_audit table for tracking import operations
-- Supports import audit trail feature (Sprint 10B - SE-10B-002)

CREATE TABLE import_audit (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,

    -- Import timestamp
    import_timestamp TIMESTAMP NOT NULL,

    -- File information
    file_name VARCHAR(255),
    file_hash VARCHAR(64),  -- SHA-256 hex string

    -- Import metadata
    import_type VARCHAR(50) NOT NULL,  -- INCOME, EXPENSE, MIXED, JSON, CSV_INCOME, CSV_EXPENSE

    -- Record counts
    total_records INTEGER NOT NULL DEFAULT 0,
    imported_count INTEGER NOT NULL DEFAULT 0,
    skipped_count INTEGER NOT NULL DEFAULT 0,

    -- Imported record IDs (JSON array of UUIDs for undo capability)
    record_ids TEXT,  -- JSON: ["uuid1", "uuid2", ...]

    -- Status tracking
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, UNDONE

    -- Undo information
    undone_at TIMESTAMP,
    undone_by VARCHAR(255),

    -- Foreign key constraint
    CONSTRAINT fk_import_audit_business FOREIGN KEY (business_id)
        REFERENCES businesses(id) ON DELETE CASCADE,

    -- Status constraint
    CONSTRAINT chk_import_audit_status CHECK (status IN ('ACTIVE', 'UNDONE'))
);

-- Indexes for common queries
CREATE INDEX idx_import_audit_business_id ON import_audit(business_id);
CREATE INDEX idx_import_audit_timestamp ON import_audit(import_timestamp);
CREATE INDEX idx_import_audit_status ON import_audit(status);
