-- SE-802: Bank Import Column Mapping Wizard
-- Migration to add column mapping preferences table for saving user mappings

-- Create table for storing column mapping preferences
CREATE TABLE column_mapping_preferences (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,

    -- Bank identifier (format name or header hash)
    bank_identifier VARCHAR(100) NOT NULL,
    mapping_name VARCHAR(100),

    -- Column mapping by index and name
    date_column_index INTEGER,
    date_column_name VARCHAR(100),
    description_column_index INTEGER,
    description_column_name VARCHAR(100),
    amount_column_index INTEGER,
    amount_column_name VARCHAR(100),
    income_column_index INTEGER,
    income_column_name VARCHAR(100),
    expense_column_index INTEGER,
    expense_column_name VARCHAR(100),
    category_column_index INTEGER,
    category_column_name VARCHAR(100),

    -- Format settings
    date_format VARCHAR(50),
    amount_interpretation VARCHAR(20) NOT NULL DEFAULT 'STANDARD',

    -- Timestamps
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NOT NULL,
    use_count INTEGER NOT NULL DEFAULT 0,

    -- Foreign key constraint
    CONSTRAINT fk_mapping_business FOREIGN KEY (business_id)
        REFERENCES businesses(id) ON DELETE CASCADE,

    -- Ensure amount_interpretation is valid
    CONSTRAINT chk_amount_interpretation
        CHECK (amount_interpretation IN ('STANDARD', 'INVERTED', 'SEPARATE_COLUMNS'))
);

-- Index for finding preferences by business
CREATE INDEX idx_mapping_pref_business ON column_mapping_preferences(business_id);

-- Index for finding preferences by bank identifier
CREATE INDEX idx_mapping_pref_bank ON column_mapping_preferences(bank_identifier);

-- Index for finding most recently used preferences
CREATE INDEX idx_mapping_pref_last_used ON column_mapping_preferences(business_id, last_used_at DESC);

-- Unique constraint: one mapping per bank identifier per business
CREATE UNIQUE INDEX idx_mapping_pref_unique
    ON column_mapping_preferences(business_id, bank_identifier);
