package io.artur.bankaccount.application.commands.models;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.math.BigDecimal;
import java.util.UUID;

public class WithdrawMoneyCommand {
    
    private final UUID accountId;
    private final BigDecimal amount;
    private final EventMetadata metadata;
    
    public WithdrawMoneyCommand(UUID accountId, BigDecimal amount, EventMetadata metadata) {
        this.accountId = accountId;
        this.amount = amount;
        this.metadata = metadata;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public EventMetadata getMetadata() {
        return metadata;
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