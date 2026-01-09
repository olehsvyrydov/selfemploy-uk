-- V1: Initial Schema for UK Self-Employment Manager
-- Creates core tables: businesses, incomes, expenses, tax_years

-- Tax Years table (reference data)
CREATE TABLE tax_years (
    id UUID PRIMARY KEY,
    start_year INT NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    label VARCHAR(10) NOT NULL,
    CONSTRAINT chk_tax_year_dates CHECK (end_date > start_date)
);

-- Businesses table
CREATE TABLE businesses (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    utr VARCHAR(10),
    accounting_period_start DATE,
    accounting_period_end DATE,
    type VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_business_dates CHECK (accounting_period_end >= accounting_period_start)
);

-- Incomes table
CREATE TABLE incomes (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    date DATE NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(50) NOT NULL,
    reference VARCHAR(100),
    CONSTRAINT fk_income_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT chk_income_amount CHECK (amount > 0)
);

-- Expenses table
CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL,
    date DATE NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(50) NOT NULL,
    receipt_path VARCHAR(500),
    notes VARCHAR(1000),
    CONSTRAINT fk_expense_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT chk_expense_amount CHECK (amount > 0)
);

-- Indexes for common queries
CREATE INDEX idx_incomes_business_date ON incomes(business_id, date);
CREATE INDEX idx_incomes_category ON incomes(business_id, category);
CREATE INDEX idx_expenses_business_date ON expenses(business_id, date);
CREATE INDEX idx_expenses_category ON expenses(business_id, category);
CREATE INDEX idx_businesses_utr ON businesses(utr);
CREATE INDEX idx_businesses_active ON businesses(active);

-- Insert common tax years
INSERT INTO tax_years (id, start_year, start_date, end_date, label) VALUES
    (RANDOM_UUID(), 2024, '2024-04-06', '2025-04-05', '2024/25'),
    (RANDOM_UUID(), 2025, '2025-04-06', '2026-04-05', '2025/26'),
    (RANDOM_UUID(), 2026, '2026-04-06', '2027-04-05', '2026/27');
