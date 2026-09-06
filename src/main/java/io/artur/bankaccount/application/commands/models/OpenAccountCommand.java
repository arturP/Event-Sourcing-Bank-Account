package io.artur.bankaccount.application.commands.models;

import java.math.BigDecimal;
import java.util.UUID;

public class OpenAccountCommand {
    
    private final UUID accountId;
    private final String accountHolder;
    private final BigDecimal overdraftLimit;
    private final CommandContext context;
    
    public OpenAccountCommand(UUID accountId, String accountHolder, BigDecimal overdraftLimit, CommandContext context) {
        this.accountId = accountId;
        this.accountHolder = accountHolder;
        this.overdraftLimit = overdraftLimit;
        this.context = context;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public String getAccountHolder() {
        return accountHolder;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public CommandContext getContext() {
        return context;
    }
    
    public void validate() {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (accountHolder == null || accountHolder.trim().isEmpty()) {
            throw new IllegalArgumentException("Account holder name cannot be null or empty");
        }
        if (overdraftLimit == null) {
            throw new IllegalArgumentException("Overdraft limit cannot be null");
        }
        if (overdraftLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative");
        }
    }
}