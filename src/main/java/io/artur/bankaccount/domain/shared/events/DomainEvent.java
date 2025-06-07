package io.artur.bankaccount.domain.shared.events;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {
    UUID getId();
    LocalDateTime getTimestamp();
    EventMetadata getMetadata();
    int getVersion();
}