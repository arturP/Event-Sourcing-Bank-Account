package io.artur.eventsourcing.events;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class AccountEventBase implements AccountEvent {

    private UUID accountId;
    private LocalDateTime timestamp;
    
    // Default constructor for Jackson
    protected AccountEventBase() {
    }

    public AccountEventBase(final UUID accountId, final LocalDateTime timestamp) {
        this.accountId = accountId;
        this.timestamp = timestamp;
    }

    @Override
    public UUID getId() {
        return accountId;
    }
    
    // Setter for Jackson deserialization
    public void setId(UUID accountId) {
        this.accountId = accountId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    // Setter for Jackson deserialization
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
