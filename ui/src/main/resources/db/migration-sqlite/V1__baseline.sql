-- Baseline schema for the SelfEmploy UK desktop SQLite store.
-- Applied by SqliteMigrationRunner as version 1. Statements are separated by semicolons;
-- line comments are stripped before splitting.
-- All tables use CREATE TABLE IF NOT EXISTS so this baseline is a no-op on a database
-- that already carries the schema (an upgrade from a pre-migration build).

CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS business (
    id TEXT PRIMARY KEY,
    name TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS expenses (
    id TEXT PRIMARY KEY,
    business_id TEXT NOT NULL,
    date TEXT NOT NULL,
    amount TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL,
    receipt_path TEXT,
    notes TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS income (
    id TEXT PRIMARY KEY,
    business_id TEXT NOT NULL,
    date TEXT NOT NULL,
    amount TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL,
    reference TEXT,
    client_name TEXT,
    status TEXT NOT NULL DEFAULT 'PAID',
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_expenses_business_date ON expenses(business_id, date);
CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category);
CREATE INDEX IF NOT EXISTS idx_income_business_date ON income(business_id, date);
CREATE INDEX IF NOT EXISTS idx_income_category ON income(category);

CREATE TABLE IF NOT EXISTS terms_acceptance (
    id TEXT PRIMARY KEY,
    tos_version TEXT NOT NULL,
    accepted_at TEXT NOT NULL,
    scroll_completed_at TEXT NOT NULL,
    application_version TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS privacy_acknowledgment (
    id TEXT PRIMARY KEY,
    privacy_version TEXT NOT NULL,
    acknowledged_at TEXT NOT NULL,
    application_version TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS submissions (
    id TEXT PRIMARY KEY,
    business_id TEXT NOT NULL,
    type TEXT NOT NULL,
    tax_year_start INTEGER NOT NULL,
    period_start TEXT NOT NULL,
    period_end TEXT NOT NULL,
    total_income TEXT NOT NULL,
    total_expenses TEXT NOT NULL,
    net_profit TEXT NOT NULL,
    status TEXT NOT NULL,
    hmrc_reference TEXT,
    error_message TEXT,
    submitted_at TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE,
    CHECK (type IN ('QUARTERLY_Q1', 'QUARTERLY_Q2', 'QUARTERLY_Q3', 'QUARTERLY_Q4', 'ANNUAL')),
    CHECK (status IN ('PENDING', 'SUBMITTED', 'ACCEPTED', 'REJECTED', 'NOT_SUBMITTED'))
);

CREATE INDEX IF NOT EXISTS idx_submissions_business ON submissions(business_id);
CREATE INDEX IF NOT EXISTS idx_submissions_tax_year ON submissions(tax_year_start);
CREATE INDEX IF NOT EXISTS idx_submissions_status ON submissions(status);

CREATE TABLE IF NOT EXISTS bank_transactions (
    id TEXT PRIMARY KEY,
    business_id TEXT NOT NULL,
    import_audit_id TEXT NOT NULL,
    source_format_id TEXT,
    date TEXT NOT NULL,
    amount TEXT NOT NULL,
    description TEXT NOT NULL,
    account_last_four TEXT,
    bank_transaction_id TEXT,
    transaction_hash TEXT NOT NULL,
    review_status TEXT NOT NULL DEFAULT 'PENDING',
    income_id TEXT,
    expense_id TEXT,
    exclusion_reason TEXT,
    is_business INTEGER,
    confidence_score TEXT,
    suggested_category TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    deleted_at TEXT,
    deleted_by TEXT,
    deletion_reason TEXT,
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE,
    CHECK (review_status IN ('PENDING', 'CATEGORIZED', 'EXCLUDED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_bank_tx_business_status ON bank_transactions(business_id, review_status);
CREATE INDEX IF NOT EXISTS idx_bank_tx_business_date ON bank_transactions(business_id, date);
CREATE INDEX IF NOT EXISTS idx_bank_tx_hash ON bank_transactions(transaction_hash);

CREATE TABLE IF NOT EXISTS transaction_modification_log (
    id TEXT PRIMARY KEY,
    bank_transaction_id TEXT NOT NULL,
    modification_type TEXT NOT NULL,
    field_name TEXT,
    previous_value TEXT,
    new_value TEXT,
    modified_by TEXT NOT NULL,
    modified_at TEXT NOT NULL,
    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id),
    CHECK (modification_type IN (
        'CATEGORIZED', 'EXCLUDED', 'RECATEGORIZED',
        'RESTORED', 'BUSINESS_PERSONAL_CHANGED', 'CATEGORY_CHANGED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_mod_log_bank_tx ON transaction_modification_log(bank_transaction_id);
CREATE INDEX IF NOT EXISTS idx_mod_log_time ON transaction_modification_log(modified_at);

CREATE TABLE IF NOT EXISTS reconciliation_matches (
    id TEXT PRIMARY KEY,
    bank_transaction_id TEXT NOT NULL,
    manual_transaction_id TEXT NOT NULL,
    manual_transaction_type TEXT NOT NULL CHECK(manual_transaction_type IN ('INCOME', 'EXPENSE')),
    confidence REAL NOT NULL,
    match_tier TEXT NOT NULL CHECK(match_tier IN ('LINKED', 'EXACT', 'LIKELY', 'POSSIBLE')),
    status TEXT NOT NULL DEFAULT 'UNRESOLVED' CHECK(status IN ('UNRESOLVED', 'CONFIRMED', 'DISMISSED')),
    business_id TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    resolved_at TEXT,
    resolved_by TEXT,
    UNIQUE(bank_transaction_id, manual_transaction_id, manual_transaction_type)
);

CREATE INDEX IF NOT EXISTS idx_recon_business ON reconciliation_matches(business_id);
CREATE INDEX IF NOT EXISTS idx_recon_status ON reconciliation_matches(business_id, status);
CREATE INDEX IF NOT EXISTS idx_recon_bank_tx ON reconciliation_matches(bank_transaction_id);
