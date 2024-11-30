package io.artur.eventsourcing.events;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccountOpenedEvent extends AccountEventBase {

    private final String accountHolder;

    public AccountOpenedEvent(UUID accountId, String accountHolder) {
        super(accountId, LocalDateTime.now());
        this.accountHolder = accountHolder;
    }

    public String getAccountHolder() {
        return accountHolder;
    }
}
