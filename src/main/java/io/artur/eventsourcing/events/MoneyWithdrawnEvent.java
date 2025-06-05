package io.artur.eventsourcing.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class MoneyWithdrawnEvent extends AccountEventBase {

    private BigDecimal amount;

    // Default constructor for Jackson
    public MoneyWithdrawnEvent() {
        super();
    }
    
    public MoneyWithdrawnEvent(final UUID accountId, final BigDecimal amount) {
        this(accountId, amount, new EventMetadata(1));
    }
    
    public MoneyWithdrawnEvent(final UUID accountId, final BigDecimal amount, final EventMetadata metadata) {
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
