package io.artur.eventsourcing.cqrs.queries;

import java.util.UUID;

public class GetAccountSummaryQuery {
    private final UUID accountId;
    
    public GetAccountSummaryQuery(UUID accountId) {
        this.accountId = accountId;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
}