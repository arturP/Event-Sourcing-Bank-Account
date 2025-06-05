package io.artur.eventsourcing.commands;

import io.artur.eventsourcing.events.EventMetadata;

import java.math.BigDecimal;
import java.util.UUID;

public class OpenAccountCommand implements Command {
    
    private final UUID aggregateId;
    private final String accountHolder;
    private final BigDecimal overdraftLimit;
    private final EventMetadata metadata;
    
    public OpenAccountCommand(UUID aggregateId, String accountHolder, BigDecimal overdraftLimit, EventMetadata metadata) {
        this.aggregateId = aggregateId;
        this.accountHolder = accountHolder;
        this.overdraftLimit = overdraftLimit;
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
    
    public String getAccountHolder() {
        return accountHolder;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    @Override
    public void validate() {
        if (aggregateId == null) {
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