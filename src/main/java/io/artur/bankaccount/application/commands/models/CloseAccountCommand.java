package io.artur.bankaccount.application.commands.models;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.util.UUID;

public class CloseAccountCommand {
    
    private final UUID accountId;
    private final String reason;
    private final String closedBy;
    private final EventMetadata metadata;
    
    public CloseAccountCommand(UUID accountId, String reason, String closedBy, EventMetadata metadata) {
        this.accountId = accountId;
        this.reason = reason;
        this.closedBy = closedBy;
        this.metadata = metadata;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getClosedBy() {
        return closedBy;
    }
    
    public EventMetadata getMetadata() {
        return metadata;
    }
    
    public void validate() {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Close reason cannot be null or empty");
        }
        if (closedBy == null || closedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Closed by cannot be null or empty");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Event metadata cannot be null");
        }
    }
}