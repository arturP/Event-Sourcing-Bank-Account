package io.artur.bankaccount.application.commands.models;

import java.util.UUID;

public class ReactivateAccountCommand {
    
    private final UUID accountId;
    private final String reason;
    private final String reactivatedBy;
    private final CommandContext context;
    
    public ReactivateAccountCommand(UUID accountId, String reason, String reactivatedBy, CommandContext context) {
        this.accountId = accountId;
        this.reason = reason;
        this.reactivatedBy = reactivatedBy;
        this.context = context;
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
    
    public CommandContext getContext() {
        return context;
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
        if (context == null) {
            throw new IllegalArgumentException("Command context cannot be null");
        }
    }
}