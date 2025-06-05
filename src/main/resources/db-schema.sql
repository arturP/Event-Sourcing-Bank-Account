-- Database schema for event-sourcing-bank-account

-- Events table
CREATE TABLE IF NOT EXISTS account_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id UUID NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data CLOB NOT NULL
);

-- Create index on account_id
CREATE INDEX IF NOT EXISTS idx_account_events_account_id ON account_events(account_id);

-- Snapshots table
CREATE TABLE IF NOT EXISTS account_snapshots (
    snapshot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id UUID NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    snapshot_data CLOB NOT NULL
);

-- Create unique constraint on account_id and snapshot_time
ALTER TABLE account_snapshots ADD CONSTRAINT IF NOT EXISTS unique_account_snapshot UNIQUE (account_id, snapshot_time);