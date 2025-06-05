package io.artur.eventsourcing.snapshots;

import java.util.Optional;
import java.util.UUID;

public interface SnapshotStore {
    
    void saveSnapshot(AccountSnapshot snapshot);
    
    Optional<AccountSnapshot> getLatestSnapshot(UUID accountId);
    
    void deleteSnapshots(UUID accountId);
}