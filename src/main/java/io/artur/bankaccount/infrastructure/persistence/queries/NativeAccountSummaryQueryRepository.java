package io.artur.bankaccount.infrastructure.persistence.queries;

import io.artur.bankaccount.application.ports.outgoing.AccountSummaryQueryRepository;
import io.artur.bankaccount.application.queries.models.AccountSearchQuery;
import io.artur.bankaccount.application.queries.readmodels.AccountSummaryReadModel;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class NativeAccountSummaryQueryRepository implements AccountSummaryQueryRepository {
    
    private final Map<UUID, AccountSummaryReadModel> storage = new ConcurrentHashMap<>();
    
    @Override
    public Optional<AccountSummaryReadModel> findByAccountId(UUID accountId) {
        return Optional.ofNullable(storage.get(accountId));
    }
    
    @Override
    public List<AccountSummaryReadModel> findAll() {
        return new ArrayList<>(storage.values());
    }
    
    @Override
    public PagedResult<AccountSummaryReadModel> search(AccountSearchQuery query) {
        List<AccountSummaryReadModel> allAccounts = findAll();
        
        // Apply filters
        List<AccountSummaryReadModel> filtered = allAccounts.stream()
            .filter(account -> matchesFilter(account, query))
            .sorted(getComparator(query.getSortBy(), query.getSortDirection()))
            .collect(Collectors.toList());
        
        // Apply pagination
        int start = query.getPage() * query.getSize();
        int end = Math.min(start + query.getSize(), filtered.size());
        
        List<AccountSummaryReadModel> pageContent = start < filtered.size() 
            ? filtered.subList(start, end) 
            : Collections.emptyList();
        
        return PagedResult.of(pageContent, query.getPage(), query.getSize(), filtered.size());
    }
    
    @Override
    public List<AccountSummaryReadModel> findByAccountHolderNameContaining(String holderName) {
        return storage.values().stream()
            .filter(account -> account.getAccountHolderName() != null && 
                             account.getAccountHolderName().toLowerCase().contains(holderName.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AccountSummaryReadModel> findByAccountStatus(String status) {
        return storage.values().stream()
            .filter(account -> status.equals(account.getAccountStatus()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AccountSummaryReadModel> findByBalanceGreaterThan(BigDecimal threshold) {
        return storage.values().stream()
            .filter(account -> account.getBalance() != null && 
                             account.getBalance().compareTo(threshold) > 0)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AccountSummaryReadModel> findByBalanceLessThan(BigDecimal threshold) {
        return storage.values().stream()
            .filter(account -> account.getBalance() != null && 
                             account.getBalance().compareTo(threshold) < 0)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<AccountSummaryReadModel> findDormantAccounts(int daysWithoutActivity) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysWithoutActivity);
        return storage.values().stream()
            .filter(account -> account.getLastTransactionDate() == null || 
                             account.getLastTransactionDate().isBefore(cutoffDate))
            .collect(Collectors.toList());
    }
    
    @Override
    public AccountStatistics getAccountStatistics() {
        List<AccountSummaryReadModel> allAccounts = findAll();
        
        long total = allAccounts.size();
        long active = allAccounts.stream().mapToLong(a -> "ACTIVE".equals(a.getAccountStatus()) ? 1 : 0).sum();
        long frozen = allAccounts.stream().mapToLong(a -> "FROZEN".equals(a.getAccountStatus()) ? 1 : 0).sum();
        long closed = allAccounts.stream().mapToLong(a -> "CLOSED".equals(a.getAccountStatus()) ? 1 : 0).sum();
        long dormant = allAccounts.stream().mapToLong(a -> "DORMANT".equals(a.getAccountStatus()) ? 1 : 0).sum();
        
        BigDecimal totalBalance = allAccounts.stream()
            .map(AccountSummaryReadModel::getBalance)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageBalance = total > 0 
            ? totalBalance.divide(BigDecimal.valueOf(total), 2, BigDecimal.ROUND_HALF_UP)
            : BigDecimal.ZERO;
        
        return new AccountStatistics(total, active, frozen, closed, dormant, totalBalance, averageBalance);
    }
    
    @Override
    public void save(AccountSummaryReadModel accountSummary) {
        storage.put(accountSummary.getAccountId(), accountSummary);
    }
    
    @Override
    public void delete(UUID accountId) {
        storage.remove(accountId);
    }
    
    @Override
    public boolean exists(UUID accountId) {
        return storage.containsKey(accountId);
    }
    
    @Override
    public long count() {
        return storage.size();
    }
    
    @Override
    public long countByStatus(String status) {
        return storage.values().stream()
            .mapToLong(account -> status.equals(account.getAccountStatus()) ? 1 : 0)
            .sum();
    }
    
    private boolean matchesFilter(AccountSummaryReadModel account, AccountSearchQuery query) {
        if (query.getAccountHolderName() != null && 
            (account.getAccountHolderName() == null || 
             !account.getAccountHolderName().toLowerCase().contains(query.getAccountHolderName().toLowerCase()))) {
            return false;
        }
        
        if (query.getAccountStatus() != null && 
            !query.getAccountStatus().equals(account.getAccountStatus())) {
            return false;
        }
        
        if (query.getMinBalance() != null && 
            (account.getBalance() == null || account.getBalance().compareTo(query.getMinBalance()) < 0)) {
            return false;
        }
        
        if (query.getMaxBalance() != null && 
            (account.getBalance() == null || account.getBalance().compareTo(query.getMaxBalance()) > 0)) {
            return false;
        }
        
        return true;
    }
    
    private Comparator<AccountSummaryReadModel> getComparator(String sortBy, String sortDirection) {
        Comparator<AccountSummaryReadModel> comparator;
        
        switch (sortBy != null ? sortBy.toLowerCase() : "accountholdername") {
            case "balance":
                comparator = Comparator.comparing(AccountSummaryReadModel::getBalance, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "status":
                comparator = Comparator.comparing(AccountSummaryReadModel::getAccountStatus, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "openeddate":
                comparator = Comparator.comparing(AccountSummaryReadModel::getAccountOpenedDate, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                comparator = Comparator.comparing(AccountSummaryReadModel::getAccountHolderName, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
        }
        
        return "desc".equalsIgnoreCase(sortDirection) ? comparator.reversed() : comparator;
    }
}