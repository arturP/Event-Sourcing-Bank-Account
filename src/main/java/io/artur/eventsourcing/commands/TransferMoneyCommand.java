package io.artur.eventsourcing.commands;

import io.artur.eventsourcing.events.EventMetadata;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferMoneyCommand implements Command {
    
    private final UUID fromAccountId;
    private final UUID toAccountId;
    private final BigDecimal amount;
    private final String description;
    private final EventMetadata metadata;
    
    public TransferMoneyCommand(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String description, EventMetadata metadata) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description;
        this.metadata = metadata;
    }
    
    @Override
    public UUID getAggregateId() {
        return fromAccountId;
    }
    
    @Override
    public EventMetadata getMetadata() {
        return metadata;
    }
    
    public UUID getFromAccountId() {
        return fromAccountId;
    }
    
    public UUID getToAccountId() {
        return toAccountId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public void validate() {
        if (fromAccountId == null) {
            throw new IllegalArgumentException("From account ID cannot be null");
        }
        if (toAccountId == null) {
            throw new IllegalArgumentException("To account ID cannot be null");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer money to the same account");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("Description cannot exceed 255 characters");
        }
    }
}