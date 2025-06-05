package io.artur.eventsourcing.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountOpenedEvent extends AccountEventBase {

    private String accountHolder;
    private BigDecimal overdraftLimit;

    // Default constructor for Jackson
    public AccountOpenedEvent() {
        super();
    }
    
    public AccountOpenedEvent(UUID accountId, String accountHolder) {
        this(accountId, accountHolder, BigDecimal.ZERO);
    }
    
    public AccountOpenedEvent(UUID accountId, String accountHolder, BigDecimal overdraftLimit) {
        this(accountId, accountHolder, overdraftLimit, new EventMetadata(1));
    }
    
    public AccountOpenedEvent(UUID accountId, String accountHolder, BigDecimal overdraftLimit, EventMetadata metadata) {
        super(accountId, LocalDateTime.now(), metadata);
        this.accountHolder = accountHolder;
        this.overdraftLimit = overdraftLimit;
    }

    public String getAccountHolder() {
        return accountHolder;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    // Setter for Jackson deserialization
    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }
    
    // Setter for Jackson deserialization
    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
}
