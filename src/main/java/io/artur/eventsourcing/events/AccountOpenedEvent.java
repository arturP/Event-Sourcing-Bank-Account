package io.artur.eventsourcing.events;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccountOpenedEvent extends AccountEventBase {

    private String accountHolder;

    // Default constructor for Jackson
    public AccountOpenedEvent() {
        super();
    }
    
    public AccountOpenedEvent(UUID accountId, String accountHolder) {
        super(accountId, LocalDateTime.now());
        this.accountHolder = accountHolder;
    }

    public String getAccountHolder() {
        return accountHolder;
    }
    
    // Setter for Jackson deserialization
    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }
}
