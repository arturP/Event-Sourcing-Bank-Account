package io.artur.bankaccount.domain.account.events;

import io.artur.bankaccount.domain.shared.events.EventMetadata;
import io.artur.bankaccount.domain.shared.valueobjects.Money;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountClosedEvent extends AccountEventBase {
    
    private final String reason;
    private final String closedBy;
    private final Instant closedAt;
    private final Money finalBalance;
    
    public AccountClosedEvent(UUID accountId, String reason, String closedBy, Money finalBalance, EventMetadata metadata) {
        super(accountId, LocalDateTime.now(), metadata);
        this.reason = reason;
        this.closedBy = closedBy;
        this.closedAt = Instant.now();
        this.finalBalance = finalBalance;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getClosedBy() {
        return closedBy;
    }
    
    public Instant getClosedAt() {
        return closedAt;
    }
    
    public Money getFinalBalance() {
        return finalBalance;
    }
    
    public String getEventType() {
        return "AccountClosed";
    }
    
    @Override
    public String toString() {
        return String.format("AccountClosedEvent{accountId=%s, reason='%s', closedBy='%s', closedAt=%s, finalBalance=%s, metadata=%s}", 
                           getId(), reason, closedBy, closedAt, finalBalance, getMetadata());
    }
}