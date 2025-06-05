package io.artur.eventsourcing.commands;

import io.artur.eventsourcing.events.EventMetadata;

import java.math.BigDecimal;
import java.util.UUID;

public class DepositMoneyCommand implements Command {
    
    private final UUID aggregateId;
    private final BigDecimal amount;
    private final EventMetadata metadata;
    
    public DepositMoneyCommand(UUID aggregateId, BigDecimal amount, EventMetadata metadata) {
        this.aggregateId = aggregateId;
        this.amount = amount;
        this.metadata = metadata;
    }
    
    @Override
    public UUID getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public EventMetadata getMetadata() {
        return metadata;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    @Override
    public void validate() {
        if (aggregateId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }
    }
}