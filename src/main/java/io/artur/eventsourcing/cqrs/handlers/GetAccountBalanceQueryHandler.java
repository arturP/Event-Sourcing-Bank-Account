package io.artur.eventsourcing.cqrs.handlers;

import io.artur.eventsourcing.cqrs.QueryHandler;
import io.artur.eventsourcing.cqrs.queries.GetAccountBalanceQuery;
import io.artur.eventsourcing.domain.Money;
import io.artur.eventsourcing.repository.BankAccountRepository;

public class GetAccountBalanceQueryHandler implements QueryHandler<GetAccountBalanceQuery, Money> {
    
    private final BankAccountRepository repository;
    
    public GetAccountBalanceQueryHandler(BankAccountRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Money handle(GetAccountBalanceQuery query) {
        return repository.findById(query.getAccountId())
                .map(account -> Money.of(account.getBalance()))
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + query.getAccountId()));
    }
}