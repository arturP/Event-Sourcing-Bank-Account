package io.artur.bankaccount.domain.account.events;

import io.artur.bankaccount.domain.account.valueobjects.AccountStatus;
import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountFrozenEvent extends AccountEventBase {
    
    private final String reason;
    private final String frozenBy;
    private final Instant frozenAt;
    
    public AccountFrozenEvent(UUID accountId, String reason, String frozenBy, EventMetadata metadata) {
        super(accountId, LocalDateTime.now(), metadata);
        this.reason = reason;
        this.frozenBy = frozenBy;
        this.frozenAt = Instant.now();
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getFrozenBy() {
        return frozenBy;
    }
    
    public Instant getFrozenAt() {
        return frozenAt;
    }
    
    public String getEventType() {
        return "AccountFrozen";
    }
    
    @Override
    public String toString() {
        return String.format("AccountFrozenEvent{accountId=%s, reason='%s', frozenBy='%s', frozenAt=%s, metadata=%s}", 
                           getId(), reason, frozenBy, frozenAt, getMetadata());
    }
}