package io.artur.bankaccount.domain.account.events;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class MoneyWithdrawnEvent extends AccountEventBase {

    private BigDecimal amount;

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
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}