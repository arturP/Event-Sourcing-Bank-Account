package io.artur.bankaccount.application.commands.models;

import java.math.BigDecimal;
import java.util.UUID;

public class WithdrawMoneyCommand {
    
    private final UUID accountId;
    private final BigDecimal amount;
    private final CommandContext context;
    
    public WithdrawMoneyCommand(UUID accountId, BigDecimal amount, CommandContext context) {
        this.accountId = accountId;
        this.amount = amount;
        this.context = context;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public CommandContext getContext() {
        return context;
    }
    
    public void validate() {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }
    }
}