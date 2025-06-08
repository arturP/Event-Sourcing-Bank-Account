package io.artur.bankaccount.domain.account.events;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountMarkedDormantEvent extends AccountEventBase {
    
    private final String reason;
    private final String markedBy;
    private final Instant markedAt;
    private final Instant lastActivity;
    
    public AccountMarkedDormantEvent(UUID accountId, String reason, String markedBy, Instant lastActivity, EventMetadata metadata) {
        super(accountId, LocalDateTime.now(), metadata);
        this.reason = reason;
        this.markedBy = markedBy;
        this.markedAt = Instant.now();
        this.lastActivity = lastActivity;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getMarkedBy() {
        return markedBy;
    }
    
    public Instant getMarkedAt() {
        return markedAt;
    }
    
    public Instant getLastActivity() {
        return lastActivity;
    }
    
    public String getEventType() {
        return "AccountMarkedDormant";
    }
    
    @Override
    public String toString() {
        return String.format("AccountMarkedDormantEvent{accountId=%s, reason='%s', markedBy='%s', markedAt=%s, lastActivity=%s, metadata=%s}", 
                           getId(), reason, markedBy, markedAt, lastActivity, getMetadata());
    }
}