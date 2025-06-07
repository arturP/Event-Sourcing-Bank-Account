package io.artur.bankaccount.domain.account.events;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountOpenedEvent extends AccountEventBase {

    private String accountHolder;
    private BigDecimal overdraftLimit;

    public AccountOpenedEvent() {
        super();
    }
    
    public AccountOpenedEvent(final UUID accountId, final String accountHolder, final BigDecimal overdraftLimit) {
        this(accountId, accountHolder, overdraftLimit, new EventMetadata(1));
    }
    
    public AccountOpenedEvent(final UUID accountId, final String accountHolder, final BigDecimal overdraftLimit, final EventMetadata metadata) {
        super(accountId, LocalDateTime.now(), metadata);
        this.accountHolder = accountHolder;
        this.overdraftLimit = overdraftLimit;
    }

    public String getAccountHolder() {
        return accountHolder;
    }
    
    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
}