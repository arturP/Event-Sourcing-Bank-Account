package io.artur.eventsourcing.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class MoneyDepositedEvent extends AccountEventBase {

    private final BigDecimal amount;

    public MoneyDepositedEvent(final UUID accountId, final BigDecimal amount) {
        super(accountId, LocalDateTime.now());
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
