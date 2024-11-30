package io.artur.eventsourcing.events;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class AccountEventBase implements AccountEvent {

    private final UUID accountId;
    private final LocalDateTime timestamp;

    public AccountEventBase(final UUID accountId, final LocalDateTime timestamp) {
        this.accountId = accountId;
        this.timestamp = timestamp;
    }

    @Override
    public UUID getId() {
        return accountId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
