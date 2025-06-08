package io.artur.bankaccount.application.commands.models;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.util.UUID;

public class FreezeAccountCommand {
    
    private final UUID accountId;
    private final String reason;
    private final String frozenBy;
    private final EventMetadata metadata;
    
    public FreezeAccountCommand(UUID accountId, String reason, String frozenBy, EventMetadata metadata) {
        this.accountId = accountId;
        this.reason = reason;
        this.frozenBy = frozenBy;
        this.metadata = metadata;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getFrozenBy() {
        return frozenBy;
    }
    
    public EventMetadata getMetadata() {
        return metadata;
    }
    
    public void validate() {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Freeze reason cannot be null or empty");
        }
        if (frozenBy == null || frozenBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Frozen by cannot be null or empty");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Event metadata cannot be null");
        }
    }
}