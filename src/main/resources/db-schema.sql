-- Database schema for event-sourcing-bank-account

-- Events table
CREATE TABLE IF NOT EXISTS account_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id UUID NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data CLOB NOT NULL
);

-- Performance indexes for account_events table
-- Primary index for event stream retrieval by account
CREATE INDEX IF NOT EXISTS idx_account_events_account_id ON account_events(account_id);

-- Composite index for account + timestamp ordering (event stream pagination)
CREATE INDEX IF NOT EXISTS idx_account_events_account_timestamp ON account_events(account_id, event_timestamp);

-- Index for event type queries (useful for event filtering and analytics)
CREATE INDEX IF NOT EXISTS idx_account_events_type ON account_events(event_type);

-- Composite index for event type + timestamp (for event type-based queries with time ordering)
CREATE INDEX IF NOT EXISTS idx_account_events_type_timestamp ON account_events(event_type, event_timestamp);

-- Index for time-based queries (useful for temporal queries and cleanup)
CREATE INDEX IF NOT EXISTS idx_account_events_timestamp ON account_events(event_timestamp);

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