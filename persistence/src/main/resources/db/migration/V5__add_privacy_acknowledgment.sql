-- V5: Add privacy acknowledgment table
-- SE-507: Privacy Notice UI
-- Stores user acknowledgments of privacy notices with version tracking

CREATE TABLE privacy_acknowledgment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    privacy_notice_version VARCHAR(20) NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_agent VARCHAR(255),
    application_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient version checking
CREATE INDEX idx_privacy_version ON privacy_acknowledgment(privacy_notice_version);

-- Index for sorting by acknowledgment time (most recent first)
CREATE INDEX idx_privacy_acknowledged_at ON privacy_acknowledgment(acknowledged_at DESC);

COMMENT ON TABLE privacy_acknowledgment IS 'Stores user acknowledgments of privacy notices for GDPR compliance';
COMMENT ON COLUMN privacy_acknowledgment.privacy_notice_version IS 'Version of the privacy notice that was acknowledged';
COMMENT ON COLUMN privacy_acknowledgment.acknowledged_at IS 'UTC timestamp when the user acknowledged the privacy notice';
COMMENT ON COLUMN privacy_acknowledgment.user_agent IS 'Optional user agent string for audit purposes';
COMMENT ON COLUMN privacy_acknowledgment.application_version IS 'Version of the application at the time of acknowledgment';
COMMENT ON COLUMN privacy_acknowledgment.created_at IS 'Record creation timestamp';
