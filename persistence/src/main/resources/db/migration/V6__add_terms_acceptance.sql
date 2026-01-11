-- SE-508: Terms of Service UI
-- Migration to add terms_acceptance table for storing ToS acceptances with version tracking

CREATE TABLE IF NOT EXISTS terms_acceptance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tos_version VARCHAR(20) NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    scroll_completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_agent VARCHAR(255),
    application_version VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for version checking (most common query)
CREATE INDEX IF NOT EXISTS idx_tos_version ON terms_acceptance(tos_version);

-- Index for finding latest acceptance
CREATE INDEX IF NOT EXISTS idx_tos_accepted_at ON terms_acceptance(accepted_at DESC);

-- Comment on table
COMMENT ON TABLE terms_acceptance IS 'Stores Terms of Service acceptances with version tracking and legal evidence (scroll timestamp)';

-- Comments on columns
COMMENT ON COLUMN terms_acceptance.tos_version IS 'Version of the Terms of Service that was accepted';
COMMENT ON COLUMN terms_acceptance.accepted_at IS 'UTC timestamp when user clicked Accept button';
COMMENT ON COLUMN terms_acceptance.scroll_completed_at IS 'UTC timestamp when user first scrolled to bottom (legal evidence)';
COMMENT ON COLUMN terms_acceptance.user_agent IS 'User agent string of the application';
COMMENT ON COLUMN terms_acceptance.application_version IS 'Version of the application at time of acceptance';
COMMENT ON COLUMN terms_acceptance.ip_address IS 'Optional IP address for legal evidence';
