package io.artur.bankaccount.application.commands.models;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.util.UUID;

public class MarkAccountDormantCommand {
    
    private final UUID accountId;
    private final String reason;
    private final String markedBy;
    private final EventMetadata metadata;
    
    public MarkAccountDormantCommand(UUID accountId, String reason, String markedBy, EventMetadata metadata) {
        this.accountId = accountId;
        this.reason = reason;
        this.markedBy = markedBy;
        this.metadata = metadata;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getMarkedBy() {
        return markedBy;
    }
    
    public EventMetadata getMetadata() {
        return metadata;
    }
    
    public void validate() {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Dormant reason cannot be null or empty");
        }
        if (markedBy == null || markedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Marked by cannot be null or empty");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Event metadata cannot be null");
        }
    }
}