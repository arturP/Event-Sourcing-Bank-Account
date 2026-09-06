package io.artur.bankaccount.application.commands.models;

import java.util.UUID;

public class FreezeAccountCommand {
    
    private final UUID accountId;
    private final String reason;
    private final String frozenBy;
    private final CommandContext context;
    
    public FreezeAccountCommand(UUID accountId, String reason, String frozenBy, CommandContext context) {
        this.accountId = accountId;
        this.reason = reason;
        this.frozenBy = frozenBy;
        this.context = context;
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
    
    public CommandContext getContext() {
        return context;
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
        if (context == null) {
            throw new IllegalArgumentException("Command context cannot be null");
        }
    }
}