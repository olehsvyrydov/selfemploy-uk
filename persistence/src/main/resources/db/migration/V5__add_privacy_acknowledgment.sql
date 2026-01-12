-- V5: Add privacy acknowledgment table
-- SE-507: Privacy Notice UI
-- Stores user acknowledgments of privacy notices with version tracking

CREATE TABLE privacy_acknowledgment (
    id UUID PRIMARY KEY,
    privacy_notice_version VARCHAR(20) NOT NULL,
    acknowledged_at TIMESTAMP NOT NULL,
    user_agent VARCHAR(255),
    application_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Index for efficient version checking
CREATE INDEX idx_privacy_version ON privacy_acknowledgment(privacy_notice_version);

-- Index for sorting by acknowledgment time (most recent first)
CREATE INDEX idx_privacy_acknowledged_at ON privacy_acknowledgment(acknowledged_at DESC);
