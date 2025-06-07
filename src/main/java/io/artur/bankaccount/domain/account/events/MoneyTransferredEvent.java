package io.artur.bankaccount.domain.account.events;

import io.artur.bankaccount.domain.shared.events.EventMetadata;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class MoneyTransferredEvent extends AccountEventBase {

    private UUID toAccountId;
    private BigDecimal amount;
    private String description;

    public MoneyTransferredEvent() {
        super();
    }
    
    public MoneyTransferredEvent(final UUID fromAccountId, final UUID toAccountId, final BigDecimal amount, final String description) {
        this(fromAccountId, toAccountId, amount, description, new EventMetadata(1));
    }
    
    public MoneyTransferredEvent(final UUID fromAccountId, final UUID toAccountId, final BigDecimal amount, final String description, final EventMetadata metadata) {
        super(fromAccountId, LocalDateTime.now(), metadata);
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }
    
    public void setToAccountId(UUID toAccountId) {
        this.toAccountId = toAccountId;
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