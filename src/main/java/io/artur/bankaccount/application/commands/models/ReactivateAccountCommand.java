package io.artur.bankaccount.application.commands.models;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.util.UUID;

public class ReactivateAccountCommand {
    
    private final UUID accountId;
    private final String reason;
    private final String reactivatedBy;
    private final EventMetadata metadata;
    
    public ReactivateAccountCommand(UUID accountId, String reason, String reactivatedBy, EventMetadata metadata) {
        this.accountId = accountId;
        this.reason = reason;
        this.reactivatedBy = reactivatedBy;
        this.metadata = metadata;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getReactivatedBy() {
        return reactivatedBy;
    }
    
    public EventMetadata getMetadata() {
        return metadata;
    }
    
    public void validate() {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reactivation reason cannot be null or empty");
        }
        if (reactivatedBy == null || reactivatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Reactivated by cannot be null or empty");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Event metadata cannot be null");
        }
    }
}