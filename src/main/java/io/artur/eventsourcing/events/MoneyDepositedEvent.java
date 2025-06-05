package io.artur.eventsourcing.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class MoneyDepositedEvent extends AccountEventBase {

    private BigDecimal amount;

    // Default constructor for Jackson
    public MoneyDepositedEvent() {
        super();
    }
    
    public MoneyDepositedEvent(final UUID accountId, final BigDecimal amount) {
        this(accountId, amount, new EventMetadata(1));
    }
    
    public MoneyDepositedEvent(final UUID accountId, final BigDecimal amount, final EventMetadata metadata) {
        super(accountId, LocalDateTime.now(), metadata);
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    
    // Setter for Jackson deserialization
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
