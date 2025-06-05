package io.artur.eventsourcing.eventstores;

import io.artur.eventsourcing.snapshots.AccountSnapshot;

import java.util.Optional;
import java.util.UUID;

public interface SnapshotCapable {

    void saveSnapshot(AccountSnapshot snapshot);
    
    Optional<AccountSnapshot> getLatestSnapshot(UUID accountId);
}