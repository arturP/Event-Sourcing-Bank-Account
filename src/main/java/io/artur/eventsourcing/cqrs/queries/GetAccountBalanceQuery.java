package io.artur.eventsourcing.cqrs.queries;

import java.util.UUID;

public class GetAccountBalanceQuery {
    private final UUID accountId;
    
    public GetAccountBalanceQuery(UUID accountId) {
        this.accountId = accountId;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
}