package io.artur.bankaccount.domain.account.events;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class AccountEventBase implements AccountDomainEvent {

    private UUID accountId;
    private LocalDateTime timestamp;
    private EventMetadata metadata;
    
    protected AccountEventBase() {
    }

    public AccountEventBase(final UUID accountId, final LocalDateTime timestamp) {
        this(accountId, timestamp, new EventMetadata(1));
    }
    
    public AccountEventBase(final UUID accountId, final LocalDateTime timestamp, final EventMetadata metadata) {
        this.accountId = accountId;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    @Override
    public UUID getId() {
        return accountId;
    }
    
    public void setId(UUID accountId) {
        this.accountId = accountId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    @Override
    public EventMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public int getVersion() {
        return metadata != null ? metadata.getVersion() : 1;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public void setMetadata(EventMetadata metadata) {
        this.metadata = metadata;
    }
}