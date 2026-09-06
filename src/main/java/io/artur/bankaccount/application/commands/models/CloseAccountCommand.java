package io.artur.bankaccount.application.commands.models;

import java.util.UUID;

public class CloseAccountCommand {
    
    private final UUID accountId;
    private final String reason;
    private final String closedBy;
    private final CommandContext context;
    
    public CloseAccountCommand(UUID accountId, String reason, String closedBy, CommandContext context) {
        this.accountId = accountId;
        this.reason = reason;
        this.closedBy = closedBy;
        this.context = context;
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
    
    public CommandContext getContext() {
        return context;
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
        if (context == null) {
            throw new IllegalArgumentException("Command context cannot be null");
        }
    }
}