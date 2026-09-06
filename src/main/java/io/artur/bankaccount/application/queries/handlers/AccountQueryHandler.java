package io.artur.bankaccount.application.queries.handlers;

import io.artur.bankaccount.application.ports.incoming.AccountSummaryQueryUseCase;
import io.artur.bankaccount.application.queries.readmodels.AccountStatistics;
import io.artur.bankaccount.application.ports.outgoing.AccountSummaryQueryRepository;
import io.artur.bankaccount.application.queries.models.AccountSearchQuery;
import io.artur.bankaccount.application.queries.models.AccountSummaryQuery;
import io.artur.bankaccount.application.queries.readmodels.AccountSummaryReadModel;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountQueryHandler implements AccountSummaryQueryUseCase {
    
    private final AccountSummaryQueryRepository repository;
    
    public AccountQueryHandler(AccountSummaryQueryRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Optional<AccountSummaryReadModel> getAccountSummary(AccountSummaryQuery query) {
        return repository.findByAccountId(query.getAccountId());
    }
    
    @Override
    public PagedResult<AccountSummaryReadModel> searchAccounts(AccountSearchQuery query) {
        return repository.search(query);
    }
    
    public List<AccountSummaryReadModel> getAllAccounts() {
        return repository.findAll();
    }
    
    public List<AccountSummaryReadModel> getAccountsByStatus(String status) {
        return repository.findByAccountStatus(status);
    }
    
    public List<AccountSummaryReadModel> getAccountsByHolderName(String holderName) {
        return repository.findByAccountHolderNameContaining(holderName);
    }
    
    public List<AccountSummaryReadModel> getHighBalanceAccounts(BigDecimal threshold) {
        return repository.findByBalanceGreaterThan(threshold);
    }
    
    public List<AccountSummaryReadModel> getLowBalanceAccounts(BigDecimal threshold) {
        return repository.findByBalanceLessThan(threshold);
    }
    
    public List<AccountSummaryReadModel> getDormantAccounts(int daysWithoutActivity) {
        return repository.findDormantAccounts(daysWithoutActivity);
    }
    
    @Override
    public AccountStatistics getAccountStatistics() {
        return repository.getAccountStatistics();
    }
    
    public boolean accountExists(UUID accountId) {
        return repository.exists(accountId);
    }
    
    public long getTotalAccountCount() {
        return repository.count();
    }
    
    public long getAccountCountByStatus(String status) {
        return repository.countByStatus(status);
    }
}