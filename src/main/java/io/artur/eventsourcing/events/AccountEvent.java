package io.artur.eventsourcing.events;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AccountEvent {
    UUID getId();
    LocalDateTime getTimestamp();
}
