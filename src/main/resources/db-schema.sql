-- Database schema for event-sourcing-bank-account

-- Events table
CREATE TABLE IF NOT EXISTS events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data CLOB NOT NULL,
    event_version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    correlation_id VARCHAR(36)
);

-- Performance indexes for events table
-- Primary index for event stream retrieval by aggregate
CREATE INDEX IF NOT EXISTS idx_events_aggregate_id ON events(aggregate_id);

-- Composite index for aggregate + version ordering (event stream retrieval)
CREATE INDEX IF NOT EXISTS idx_events_aggregate_version ON events(aggregate_id, event_version);

-- Index for event type queries (useful for event filtering and analytics)
CREATE INDEX IF NOT EXISTS idx_events_type ON events(event_type);

-- Composite index for event type + timestamp (for event type-based queries with time ordering)
CREATE INDEX IF NOT EXISTS idx_events_type_timestamp ON events(event_type, created_at);

-- Index for time-based queries (useful for temporal queries and cleanup)
CREATE INDEX IF NOT EXISTS idx_events_timestamp ON events(created_at);

-- Snapshots table
CREATE TABLE IF NOT EXISTS account_snapshots (
    snapshot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id UUID NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    snapshot_data CLOB NOT NULL
);

-- Performance indexes for account_snapshots table
-- Index for finding latest snapshot by account
CREATE INDEX IF NOT EXISTS idx_account_snapshots_account_time ON account_snapshots(account_id, snapshot_time DESC);

-- Index for snapshot cleanup operations
CREATE INDEX IF NOT EXISTS idx_account_snapshots_time ON account_snapshots(snapshot_time);

-- Create unique constraint on account_id and snapshot_time
ALTER TABLE account_snapshots ADD CONSTRAINT IF NOT EXISTS unique_account_snapshot UNIQUE (account_id, snapshot_time);