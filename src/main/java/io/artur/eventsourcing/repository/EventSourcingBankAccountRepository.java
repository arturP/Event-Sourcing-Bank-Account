package io.artur.eventsourcing.repository;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.cache.CachedReadModelService;
import io.artur.eventsourcing.domain.AccountNumber;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.projections.AccountSummaryProjection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EventSourcingBankAccountRepository implements BankAccountRepository {
    
    private final EventStore<AccountEvent, UUID> eventStore;
    private final ConcurrentMap<UUID, BankAccount> cache;
    private final ConcurrentMap<AccountNumber, UUID> accountNumberIndex;
    private final CachedReadModelService readModelCache;
    
    public EventSourcingBankAccountRepository(EventStore<AccountEvent, UUID> eventStore) {
        this.eventStore = eventStore;
        this.cache = new ConcurrentHashMap<>();
        this.accountNumberIndex = new ConcurrentHashMap<>();
        this.readModelCache = new CachedReadModelService(eventStore);
    }
    
    public EventSourcingBankAccountRepository(EventStore<AccountEvent, UUID> eventStore, 
                                            CachedReadModelService.CacheConfiguration cacheConfig) {
        this.eventStore = eventStore;
        this.cache = new ConcurrentHashMap<>();
        this.accountNumberIndex = new ConcurrentHashMap<>();
        this.readModelCache = new CachedReadModelService(eventStore, cacheConfig);
    }
    
    @Override
    public void save(BankAccount aggregate) {
        if (aggregate == null || aggregate.getAccountId() == null) {
            throw new IllegalArgumentException("Cannot save null aggregate or aggregate without ID");
        }
        
        cache.put(aggregate.getAccountId(), aggregate);
        
        // Update account number index if available
        if (aggregate.getAccountNumber() != null) {
            accountNumberIndex.put(aggregate.getAccountNumber(), aggregate.getAccountId());
        }
        
        // Update read model cache
        readModelCache.updateBalance(aggregate.getAccountId(), aggregate.getBalance());
    }
    
    @Override
    public Optional<BankAccount> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        
        // Check cache first
        BankAccount cached = cache.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        // Load from event store
        try {
            BankAccount account = BankAccount.loadFromStore(eventStore, id);
            cache.put(id, account);
            
            if (account.getAccountNumber() != null) {
                accountNumberIndex.put(account.getAccountNumber(), id);
            }
            
            return Optional.of(account);
        } catch (IllegalArgumentException e) {
            // Account doesn't exist
            return Optional.empty();
        }
    }
    
    @Override
    public List<BankAccount> findAll() {
        List<BankAccount> allAccounts = new ArrayList<>();
        
        // This is a simplified implementation - in practice, you'd query the event store
        // for all account IDs and then load each one
        for (BankAccount account : cache.values()) {
            allAccounts.add(account);
        }
        
        return allAccounts;
    }
    
    @Override
    public void delete(UUID id) {
        if (id == null) {
            return;
        }
        
        BankAccount account = cache.remove(id);
        if (account != null && account.getAccountNumber() != null) {
            accountNumberIndex.remove(account.getAccountNumber());
        }
        
        // Note: In a real implementation, you might mark the account as deleted
        // rather than actually deleting events, as events are immutable
    }
    
    @Override
    public boolean exists(UUID id) {
        return findById(id).isPresent();
    }
    
    @Override
    public Optional<BankAccount> findByAccountNumber(AccountNumber accountNumber) {
        if (accountNumber == null) {
            return Optional.empty();
        }
        
        UUID accountId = accountNumberIndex.get(accountNumber);
        if (accountId != null) {
            return findById(accountId);
        }
        
        // If not in index, search through all accounts
        return findAll().stream()
                .filter(account -> accountNumber.equals(account.getAccountNumber()))
                .findFirst();
    }
    
    @Override
    public List<BankAccount> findByAccountHolder(String accountHolderName) {
        if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return findAll().stream()
                .filter(account -> accountHolderName.equalsIgnoreCase(account.getAccountHolder()))
                .toList();
    }
    
    @Override
    public List<BankAccount> findAccountsWithOverdraft() {
        return findAll().stream()
                .filter(account -> account.getOverdraftLimit().compareTo(account.getBalance().negate()) > 0)
                .toList();
    }
    
    @Override
    public List<BankAccount> findAccountsWithNegativeBalance() {
        return findAll().stream()
                .filter(account -> account.getBalance().signum() < 0)
                .toList();
    }
    
    public void clearCache() {
        cache.clear();
        accountNumberIndex.clear();
        readModelCache.invalidateAll();
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
    // Fast balance lookup using read model cache
    public Optional<BigDecimal> getCachedBalance(UUID accountId) {
        return readModelCache.getCachedBalance(accountId);
    }
    
    public BigDecimal getBalance(UUID accountId) {
        return readModelCache.getOrCreateBalance(accountId, this::loadAccountFromStore);
    }
    
    // Fast account summary lookup using read model cache
    public Optional<AccountSummaryProjection> getCachedAccountSummary(UUID accountId) {
        return readModelCache.getCachedAccountSummary(accountId);
    }
    
    public AccountSummaryProjection getAccountSummary(UUID accountId) {
        return readModelCache.getOrCreateAccountSummary(accountId, this::loadAccountFromStore);
    }
    
    // Cache statistics for monitoring
    public CachedReadModelService.CacheStatistics getCacheStatistics() {
        return readModelCache.getCacheStatistics();
    }
    
    private BankAccount loadAccountFromStore(UUID accountId) {
        return BankAccount.loadFromStore(eventStore, accountId);
    }
}