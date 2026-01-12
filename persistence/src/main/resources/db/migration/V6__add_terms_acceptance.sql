-- SE-508: Terms of Service UI
-- Migration to add terms_acceptance table for storing ToS acceptances with version tracking

CREATE TABLE IF NOT EXISTS terms_acceptance (
    id UUID PRIMARY KEY,
    tos_version VARCHAR(20) NOT NULL,
    accepted_at TIMESTAMP NOT NULL,
    scroll_completed_at TIMESTAMP NOT NULL,
    user_agent VARCHAR(255),
    application_version VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL
);

-- Index for version checking (most common query)
CREATE INDEX IF NOT EXISTS idx_tos_version ON terms_acceptance(tos_version);

-- Index for finding latest acceptance
CREATE INDEX IF NOT EXISTS idx_tos_accepted_at ON terms_acceptance(accepted_at DESC);
