package io.artur.bankaccount.application.ports.outgoing;

import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.valueobjects.Money;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Port for caching operations in the new domain model
 * This abstracts away the specific caching implementation
 */
public interface CachePort {
    
    /**
     * Account summary caching operations
     */
    Optional<AccountSummary> getCachedAccountSummary(UUID accountId);
    AccountSummary getOrCreateAccountSummary(UUID accountId, Function<UUID, BankAccount> accountLoader);
    CompletableFuture<AccountSummary> getOrCreateAccountSummaryAsync(UUID accountId, Function<UUID, BankAccount> accountLoader);
    void updateAccountSummary(UUID accountId, AccountSummary summary);
    
    /**
     * Balance caching operations
     */
    Optional<Money> getCachedBalance(UUID accountId);
    Money getOrCreateBalance(UUID accountId, Function<UUID, BankAccount> accountLoader);
    void updateBalance(UUID accountId, Money newBalance);
    
    /**
     * Account lookup caching operations
     */
    Optional<List<UUID>> getCachedAccountsByHolder(String accountHolder);
    List<UUID> getOrCreateAccountsByHolder(String accountHolder, Supplier<List<UUID>> accountLoader);
    void updateAccountsByHolder(String accountHolder, List<UUID> accountIds);
    
    /**
     * Event count caching operations
     */
    Optional<Long> getCachedEventCount(UUID accountId);
    Long getOrCreateEventCount(UUID accountId, Supplier<Long> countLoader);
    void updateEventCount(UUID accountId, long eventCount);
    
    /**
     * Cache invalidation operations
     */
    void invalidateAccount(UUID accountId);
    void invalidateAccountHolder(String accountHolder);
    void invalidateAll();
    
    /**
     * Cache maintenance and statistics
     */
    void cleanUp();
    CacheStatistics getStatistics();
    
    /**
     * Account summary for caching
     */
    class AccountSummary {
        private final UUID accountId;
        private final String accountHolder;
        private final Money overdraftLimit;
        private final java.time.LocalDateTime accountOpenedDate;
        private Money currentBalance;
        private long transactionCount;
        private java.time.LocalDateTime lastTransactionDate;
        
        public AccountSummary(UUID accountId, String accountHolder, Money overdraftLimit, 
                            java.time.LocalDateTime accountOpenedDate) {
            this.accountId = accountId;
            this.accountHolder = accountHolder;
            this.overdraftLimit = overdraftLimit;
            this.accountOpenedDate = accountOpenedDate;
        }
        
        // Getters
        public UUID getAccountId() { return accountId; }
        public String getAccountHolder() { return accountHolder; }
        public Money getOverdraftLimit() { return overdraftLimit; }
        public java.time.LocalDateTime getAccountOpenedDate() { return accountOpenedDate; }
        public Money getCurrentBalance() { return currentBalance; }
        public long getTransactionCount() { return transactionCount; }
        public java.time.LocalDateTime getLastTransactionDate() { return lastTransactionDate; }
        
        // Setters
        public void setCurrentBalance(Money currentBalance) { this.currentBalance = currentBalance; }
        public void setTransactionCount(long transactionCount) { this.transactionCount = transactionCount; }
        public void setLastTransactionDate(java.time.LocalDateTime lastTransactionDate) { 
            this.lastTransactionDate = lastTransactionDate; 
        }
    }
    
    /**
     * Cache statistics for monitoring
     */
    class CacheStatistics {
        private final double overallHitRate;
        private final long totalRequests;
        private final long totalHits;
        private final long totalMisses;
        private final CacheTypeStats accountSummaryStats;
        private final CacheTypeStats balanceStats;
        private final CacheTypeStats accountsByHolderStats;
        private final CacheTypeStats eventCountStats;
        
        public CacheStatistics(double overallHitRate, long totalRequests, long totalHits, long totalMisses,
                             CacheTypeStats accountSummaryStats, CacheTypeStats balanceStats,
                             CacheTypeStats accountsByHolderStats, CacheTypeStats eventCountStats) {
            this.overallHitRate = overallHitRate;
            this.totalRequests = totalRequests;
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.accountSummaryStats = accountSummaryStats;
            this.balanceStats = balanceStats;
            this.accountsByHolderStats = accountsByHolderStats;
            this.eventCountStats = eventCountStats;
        }
        
        // Getters
        public double getOverallHitRate() { return overallHitRate; }
        public long getTotalRequests() { return totalRequests; }
        public long getTotalHits() { return totalHits; }
        public long getTotalMisses() { return totalMisses; }
        public CacheTypeStats getAccountSummaryStats() { return accountSummaryStats; }
        public CacheTypeStats getBalanceStats() { return balanceStats; }
        public CacheTypeStats getAccountsByHolderStats() { return accountsByHolderStats; }
        public CacheTypeStats getEventCountStats() { return eventCountStats; }
    }
    
    /**
     * Statistics for a specific cache type
     */
    class CacheTypeStats {
        private final long requestCount;
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final long evictionCount;
        
        public CacheTypeStats(long requestCount, long hitCount, long missCount, double hitRate, long evictionCount) {
            this.requestCount = requestCount;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.evictionCount = evictionCount;
        }
        
        // Getters
        public long getRequestCount() { return requestCount; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
        public long getEvictionCount() { return evictionCount; }
    }
}