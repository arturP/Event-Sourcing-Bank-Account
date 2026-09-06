package io.artur.bankaccount.application.commands.models;

import java.util.UUID;

public class MarkAccountDormantCommand {
    
    private final UUID accountId;
    private final String reason;
    private final String markedBy;
    private final CommandContext context;
    
    public MarkAccountDormantCommand(UUID accountId, String reason, String markedBy, CommandContext context) {
        this.accountId = accountId;
        this.reason = reason;
        this.markedBy = markedBy;
        this.context = context;
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
    
    public CommandContext getContext() {
        return context;
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
        if (context == null) {
            throw new IllegalArgumentException("Command context cannot be null");
        }
    }
}