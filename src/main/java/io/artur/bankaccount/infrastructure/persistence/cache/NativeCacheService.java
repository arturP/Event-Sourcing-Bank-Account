package io.artur.bankaccount.infrastructure.persistence.cache;

import io.artur.bankaccount.application.ports.outgoing.CachePort;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.valueobjects.Money;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Native cache service implementation that directly implements CachePort
 * without depending on legacy infrastructure
 */
@Component
public class NativeCacheService implements CachePort {
    
    private final Map<UUID, CachedBalance> balanceCache = new ConcurrentHashMap<>();
    private final Map<UUID, CachedAccountSummary> summaryCache = new ConcurrentHashMap<>();
    
    // Cache statistics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    
    // Cache configuration
    private final long cacheExpirationMinutes = 30;
    private final int maxCacheSize = 1000;
    
    @Override
    public Optional<Money> getCachedBalance(UUID accountId) {
        totalRequests.incrementAndGet();
        
        CachedBalance cached = balanceCache.get(accountId);
        if (cached != null && !isExpired(cached.timestamp)) {
            cacheHits.incrementAndGet();
            return Optional.of(cached.balance);
        }
        
        cacheMisses.incrementAndGet();
        return Optional.empty();
    }
    
    @Override
    public Money getOrCreateBalance(UUID accountId, Function<UUID, BankAccount> accountLoader) {
        Optional<Money> cached = getCachedBalance(accountId);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        BankAccount account = accountLoader.apply(accountId);
        Money balance = account.getBalance();
        updateBalance(accountId, balance);
        return balance;
    }
    
    @Override
    public void updateBalance(UUID accountId, Money balance) {
        // Implement cache size limit with LRU eviction
        if (balanceCache.size() >= maxCacheSize && !balanceCache.containsKey(accountId)) {
            evictOldestBalance();
        }
        
        balanceCache.put(accountId, new CachedBalance(balance, LocalDateTime.now()));
    }
    
    @Override
    public Optional<AccountSummary> getCachedAccountSummary(UUID accountId) {
        totalRequests.incrementAndGet();
        
        CachedAccountSummary cached = summaryCache.get(accountId);
        if (cached != null && !isExpired(cached.timestamp)) {
            cacheHits.incrementAndGet();
            return Optional.of(cached.summary);
        }
        
        cacheMisses.incrementAndGet();
        return Optional.empty();
    }
    
    @Override
    public AccountSummary getOrCreateAccountSummary(UUID accountId, Function<UUID, BankAccount> accountLoader) {
        Optional<AccountSummary> cached = getCachedAccountSummary(accountId);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        BankAccount account = accountLoader.apply(accountId);
        AccountSummary summary = createAccountSummary(account);
        updateAccountSummary(accountId, summary);
        return summary;
    }
    
    @Override
    public CompletableFuture<AccountSummary> getOrCreateAccountSummaryAsync(UUID accountId, Function<UUID, BankAccount> accountLoader) {
        return CompletableFuture.supplyAsync(() -> getOrCreateAccountSummary(accountId, accountLoader));
    }
    
    @Override
    public void updateAccountSummary(UUID accountId, AccountSummary summary) {
        // Implement cache size limit with LRU eviction
        if (summaryCache.size() >= maxCacheSize && !summaryCache.containsKey(accountId)) {
            evictOldestSummary();
        }
        
        summaryCache.put(accountId, new CachedAccountSummary(summary, LocalDateTime.now()));
    }
    
    @Override
    public Optional<List<UUID>> getCachedAccountsByHolder(String accountHolder) {
        // Simplified implementation - would need proper indexing in real system
        return Optional.empty();
    }
    
    @Override
    public List<UUID> getOrCreateAccountsByHolder(String accountHolder, Supplier<List<UUID>> accountLoader) {
        return accountLoader.get();
    }
    
    @Override
    public void updateAccountsByHolder(String accountHolder, List<UUID> accountIds) {
        // Simplified implementation
    }
    
    @Override
    public Optional<Long> getCachedEventCount(UUID accountId) {
        // Simplified implementation
        return Optional.empty();
    }
    
    @Override
    public Long getOrCreateEventCount(UUID accountId, Supplier<Long> countLoader) {
        return countLoader.get();
    }
    
    @Override
    public void updateEventCount(UUID accountId, long eventCount) {
        // Simplified implementation
    }
    
    @Override
    public void invalidateAccount(UUID accountId) {
        boolean balanceRemoved = balanceCache.remove(accountId) != null;
        boolean summaryRemoved = summaryCache.remove(accountId) != null;
        
        if (balanceRemoved || summaryRemoved) {
            evictions.incrementAndGet();
        }
    }
    
    @Override
    public void invalidateAccountHolder(String accountHolder) {
        // Simplified implementation
    }
    
    @Override
    public void invalidateAll() {
        long totalEvicted = balanceCache.size() + summaryCache.size();
        
        balanceCache.clear();
        summaryCache.clear();
        
        evictions.addAndGet(totalEvicted);
    }
    
    @Override
    public void cleanUp() {
        // Remove expired entries
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(cacheExpirationMinutes);
        
        balanceCache.entrySet().removeIf(entry -> entry.getValue().timestamp.isBefore(cutoff));
        summaryCache.entrySet().removeIf(entry -> entry.getValue().timestamp.isBefore(cutoff));
    }
    
    @Override
    public CacheStatistics getStatistics() {
        double hitRate = totalRequests.get() > 0 ? 
            (double) cacheHits.get() / totalRequests.get() : 0.0;
        
        CacheTypeStats defaultStats = new CacheTypeStats(0, 0, 0, 0.0, 0);
        
        return new CacheStatistics(
            hitRate, totalRequests.get(), cacheHits.get(), cacheMisses.get(),
            defaultStats, defaultStats, defaultStats, defaultStats
        );
    }
    
    private AccountSummary createAccountSummary(BankAccount account) {
        return new AccountSummary(
            account.getAccountId(),
            account.getAccountHolder().getFullName(),
            account.getOverdraftLimit(),
            LocalDateTime.now()
        );
    }
    
    private boolean isExpired(LocalDateTime timestamp) {
        return timestamp.isBefore(LocalDateTime.now().minusMinutes(cacheExpirationMinutes));
    }
    
    private void evictOldestBalance() {
        balanceCache.entrySet().stream()
            .min(Map.Entry.comparingByValue((a, b) -> a.timestamp.compareTo(b.timestamp)))
            .ifPresent(entry -> {
                balanceCache.remove(entry.getKey());
                evictions.incrementAndGet();
            });
    }
    
    private void evictOldestSummary() {
        summaryCache.entrySet().stream()
            .min(Map.Entry.comparingByValue((a, b) -> a.timestamp.compareTo(b.timestamp)))
            .ifPresent(entry -> {
                summaryCache.remove(entry.getKey());
                evictions.incrementAndGet();
            });
    }
    
    // Inner classes for cached data
    private static class CachedBalance {
        final Money balance;
        final LocalDateTime timestamp;
        
        CachedBalance(Money balance, LocalDateTime timestamp) {
            this.balance = balance;
            this.timestamp = timestamp;
        }
    }
    
    private static class CachedAccountSummary {
        final AccountSummary summary;
        final LocalDateTime timestamp;
        
        CachedAccountSummary(AccountSummary summary, LocalDateTime timestamp) {
            this.summary = summary;
            this.timestamp = timestamp;
        }
    }
}