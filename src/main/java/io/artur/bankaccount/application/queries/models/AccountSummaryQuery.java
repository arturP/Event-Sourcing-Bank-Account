package io.artur.bankaccount.application.queries.models;

import java.util.UUID;

public class AccountSummaryQuery {
    
    private final UUID accountId;
    
    public AccountSummaryQuery(UUID accountId) {
        this.accountId = accountId;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public void validate() {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
    }
}