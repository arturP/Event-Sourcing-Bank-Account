package io.artur.eventsourcing.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class MoneyReceivedEvent extends AccountEventBase {

    private UUID fromAccountId;
    private BigDecimal amount;
    private String description;

    public MoneyReceivedEvent() {
        super();
    }
    
    public MoneyReceivedEvent(final UUID toAccountId, final UUID fromAccountId, final BigDecimal amount, final String description) {
        this(toAccountId, fromAccountId, amount, description, new EventMetadata(1));
    }
    
    public MoneyReceivedEvent(final UUID toAccountId, final UUID fromAccountId, final BigDecimal amount, final String description, final EventMetadata metadata) {
        super(toAccountId, LocalDateTime.now(), metadata);
        this.fromAccountId = fromAccountId;
        this.amount = amount;
        this.description = description;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }
    
    public void setFromAccountId(UUID fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}