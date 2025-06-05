package io.artur.eventsourcing.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.projections.AccountSummaryProjection;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.logging.Logger;

public class CachedReadModelService {
    
    private static final Logger LOGGER = Logger.getLogger(CachedReadModelService.class.getName());
    
    private final Cache<UUID, AccountSummaryProjection> accountSummaryCache;
    private final Cache<UUID, BigDecimal> balanceCache;
    private final Cache<String, List<UUID>> accountsByHolderCache;
    private final Cache<String, Long> eventCountCache;
    private final EventStore<AccountEvent, UUID> eventStore;
    
    public CachedReadModelService(EventStore<AccountEvent, UUID> eventStore) {
        this(eventStore, CacheConfiguration.defaultConfig());
    }
    
    public CachedReadModelService(EventStore<AccountEvent, UUID> eventStore, CacheConfiguration config) {
        this.eventStore = eventStore;
        
        this.accountSummaryCache = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize())
                .expireAfterWrite(config.getExpireAfterWrite())
                .expireAfterAccess(config.getExpireAfterAccess())
                .recordStats()
                .removalListener((key, value, cause) -> 
                    LOGGER.fine("Account summary cache eviction: " + key + " - " + cause))
                .build();
                
        this.balanceCache = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize() * 2) // Balance cache can be larger
                .expireAfterWrite(config.getExpireAfterWrite())
                .expireAfterAccess(config.getExpireAfterAccess())
                .recordStats()
                .build();
                
        this.accountsByHolderCache = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize() / 2) // Smaller for account holder queries
                .expireAfterWrite(config.getExpireAfterWrite())
                .expireAfterAccess(config.getExpireAfterAccess())
                .recordStats()
                .build();
                
        this.eventCountCache = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize())
                .expireAfterWrite(Duration.ofMinutes(5)) // Event count changes less frequently
                .recordStats()
                .build();
                
        LOGGER.info("CachedReadModelService initialized with max size: " + config.getMaxSize());
    }
    
    public Optional<AccountSummaryProjection> getCachedAccountSummary(UUID accountId) {
        return Optional.ofNullable(accountSummaryCache.getIfPresent(accountId));
    }
    
    public AccountSummaryProjection getOrCreateAccountSummary(UUID accountId, Function<UUID, BankAccount> accountLoader) {
        return accountSummaryCache.get(accountId, id -> {
            LOGGER.fine("Cache miss for account summary: " + id);
            BankAccount account = accountLoader.apply(id);
            AccountSummaryProjection projection = new AccountSummaryProjection(
                account.getAccountId(),
                account.getAccountHolder(),
                account.getOverdraftLimit(),
                LocalDateTime.now()
            );
            projection.setCurrentBalance(account.getBalance());
            return projection;
        });
    }
    
    public CompletableFuture<AccountSummaryProjection> getOrCreateAccountSummaryAsync(
            UUID accountId, 
            Function<UUID, BankAccount> accountLoader) {
        return getOrCreateAccountSummaryAsync(accountId, accountLoader, ForkJoinPool.commonPool());
    }
    
    public CompletableFuture<AccountSummaryProjection> getOrCreateAccountSummaryAsync(
            UUID accountId, 
            Function<UUID, BankAccount> accountLoader,
            Executor executor) {
        
        AccountSummaryProjection cached = accountSummaryCache.getIfPresent(accountId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return CompletableFuture.supplyAsync(() -> getOrCreateAccountSummary(accountId, accountLoader), executor);
    }
    
    public Optional<BigDecimal> getCachedBalance(UUID accountId) {
        return Optional.ofNullable(balanceCache.getIfPresent(accountId));
    }
    
    public BigDecimal getOrCreateBalance(UUID accountId, Function<UUID, BankAccount> accountLoader) {
        return balanceCache.get(accountId, id -> {
            LOGGER.fine("Cache miss for balance: " + id);
            return accountLoader.apply(id).getBalance();
        });
    }
    
    public void updateBalance(UUID accountId, BigDecimal newBalance) {
        balanceCache.put(accountId, newBalance);
        
        // Also update account summary if it exists
        AccountSummaryProjection summary = accountSummaryCache.getIfPresent(accountId);
        if (summary != null) {
            AccountSummaryProjection updated = new AccountSummaryProjection(
                summary.getAccountId(),
                summary.getAccountHolder(),
                summary.getOverdraftLimit(),
                summary.getAccountOpenedDate()
            );
            updated.setCurrentBalance(newBalance);
            updated.setTransactionCount(summary.getTransactionCount());
            updated.setLastTransactionDate(LocalDateTime.now());
            accountSummaryCache.put(accountId, updated);
        }
    }
    
    public void updateAccountSummary(UUID accountId, AccountSummaryProjection summary) {
        accountSummaryCache.put(accountId, summary);
        balanceCache.put(accountId, summary.getCurrentBalance());
    }
    
    public void invalidateAccount(UUID accountId) {
        accountSummaryCache.invalidate(accountId);
        balanceCache.invalidate(accountId);
        
        // Clear account holder cache entries that might contain this account
        accountsByHolderCache.invalidateAll();
        
        LOGGER.fine("Invalidated cache entries for account: " + accountId);
    }
    
    public void invalidateAccountHolder(String accountHolder) {
        accountsByHolderCache.invalidate(accountHolder);
        LOGGER.fine("Invalidated account holder cache: " + accountHolder);
    }
    
    public void invalidateAll() {
        accountSummaryCache.invalidateAll();
        balanceCache.invalidateAll();
        accountsByHolderCache.invalidateAll();
        eventCountCache.invalidateAll();
        LOGGER.info("Invalidated all cache entries");
    }
    
    public Long getCachedEventCount(UUID accountId) {
        return eventCountCache.getIfPresent(accountId.toString());
    }
    
    public Long getOrCreateEventCount(UUID accountId) {
        return eventCountCache.get(accountId.toString(), key -> {
            LOGGER.fine("Cache miss for event count: " + key);
            return eventStore.eventsCount(accountId);
        });
    }
    
    public void updateEventCount(UUID accountId, long eventCount) {
        eventCountCache.put(accountId.toString(), eventCount);
    }
    
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(
            accountSummaryCache.stats(),
            balanceCache.stats(),
            accountsByHolderCache.stats(),
            eventCountCache.stats()
        );
    }
    
    public void cleanUp() {
        accountSummaryCache.cleanUp();
        balanceCache.cleanUp();
        accountsByHolderCache.cleanUp();
        eventCountCache.cleanUp();
    }
    
    public static class CacheConfiguration {
        private final long maxSize;
        private final Duration expireAfterWrite;
        private final Duration expireAfterAccess;
        
        public CacheConfiguration(long maxSize, Duration expireAfterWrite, Duration expireAfterAccess) {
            this.maxSize = maxSize;
            this.expireAfterWrite = expireAfterWrite;
            this.expireAfterAccess = expireAfterAccess;
        }
        
        public static CacheConfiguration defaultConfig() {
            return new CacheConfiguration(1000, Duration.ofMinutes(30), Duration.ofMinutes(10));
        }
        
        public static CacheConfiguration performanceConfig() {
            return new CacheConfiguration(5000, Duration.ofHours(1), Duration.ofMinutes(30));
        }
        
        public static CacheConfiguration testConfig() {
            return new CacheConfiguration(100, Duration.ofMinutes(1), Duration.ofSeconds(30));
        }
        
        public long getMaxSize() { return maxSize; }
        public Duration getExpireAfterWrite() { return expireAfterWrite; }
        public Duration getExpireAfterAccess() { return expireAfterAccess; }
    }
    
    public static class CacheStatistics {
        private final CacheStats accountSummaryStats;
        private final CacheStats balanceStats;
        private final CacheStats accountsByHolderStats;
        private final CacheStats eventCountStats;
        
        public CacheStatistics(CacheStats accountSummaryStats, CacheStats balanceStats, 
                             CacheStats accountsByHolderStats, CacheStats eventCountStats) {
            this.accountSummaryStats = accountSummaryStats;
            this.balanceStats = balanceStats;
            this.accountsByHolderStats = accountsByHolderStats;
            this.eventCountStats = eventCountStats;
        }
        
        public double getOverallHitRate() {
            long totalHits = accountSummaryStats.hitCount() + balanceStats.hitCount() + 
                           accountsByHolderStats.hitCount() + eventCountStats.hitCount();
            long totalRequests = accountSummaryStats.requestCount() + balanceStats.requestCount() + 
                               accountsByHolderStats.requestCount() + eventCountStats.requestCount();
            return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
        }
        
        public CacheStats getAccountSummaryStats() { return accountSummaryStats; }
        public CacheStats getBalanceStats() { return balanceStats; }
        public CacheStats getAccountsByHolderStats() { return accountsByHolderStats; }
        public CacheStats getEventCountStats() { return eventCountStats; }
    }
}