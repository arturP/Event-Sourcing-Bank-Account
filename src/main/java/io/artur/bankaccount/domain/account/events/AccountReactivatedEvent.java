package io.artur.bankaccount.domain.account.events;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountReactivatedEvent extends AccountEventBase {
    
    private final String reason;
    private final String reactivatedBy;
    private final Instant reactivatedAt;
    private final String previousStatus;
    
    public AccountReactivatedEvent(UUID accountId, String reason, String reactivatedBy, String previousStatus, EventMetadata metadata) {
        super(accountId, LocalDateTime.now(), metadata);
        this.reason = reason;
        this.reactivatedBy = reactivatedBy;
        this.reactivatedAt = Instant.now();
        this.previousStatus = previousStatus;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getReactivatedBy() {
        return reactivatedBy;
    }
    
    public Instant getReactivatedAt() {
        return reactivatedAt;
    }
    
    public String getPreviousStatus() {
        return previousStatus;
    }
    
    public String getEventType() {
        return "AccountReactivated";
    }
    
    @Override
    public String toString() {
        return String.format("AccountReactivatedEvent{accountId=%s, reason='%s', reactivatedBy='%s', reactivatedAt=%s, previousStatus='%s', metadata=%s}", 
                           getId(), reason, reactivatedBy, reactivatedAt, previousStatus, getMetadata());
    }
}