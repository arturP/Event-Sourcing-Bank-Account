package io.artur.bankaccount.application.ports.outgoing;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Port for metrics collection operations in the new domain model
 * This abstracts away the specific metrics implementation
 */
public interface MetricsPort {
    
    /**
     * Event store operation metrics
     */
    void recordEventSave(Runnable saveOperation);
    <T> T recordEventLoad(Supplier<T> loadOperation);
    void recordEventBatchSize(int batchSize);
    
    /**
     * Cache operation metrics
     */
    void recordCacheHit(String cacheType);
    void recordCacheMiss(String cacheType);
    void recordCacheEviction(String cacheType);
    
    /**
     * Command processing metrics
     */
    void recordCommandProcessing(Runnable commandOperation);
    <T> T recordCommandProcessing(Supplier<T> commandOperation);
    
    /**
     * Aggregate operation metrics
     */
    <T> T recordAggregateRehydration(Supplier<T> rehydrationOperation);
    void recordAggregateSnapshot(Runnable snapshotOperation);
    
    /**
     * Business operation metrics
     */
    void recordAccountCreation();
    void recordDeposit();
    void recordWithdrawal();
    void recordTransfer();
    void recordOverdraftAttempt();
    void recordAccountStatusChange(String newStatus);
    
    /**
     * Custom timing operations
     */
    TimingContext startTiming(String operationName);
    void recordCustomMetric(String metricName, double value);
    void recordCustomMetric(String metricName, long value);
    
    /**
     * Performance summary and reporting
     */
    PerformanceSummary getPerformanceSummary();
    void logPerformanceSummary();
    void startPeriodicReporting(Duration period);
    void stopPeriodicReporting();
    
    /**
     * Context for timing operations
     */
    interface TimingContext extends AutoCloseable {
        Duration getElapsed();
        void addTag(String key, String value);
        @Override
        void close();
    }
    
    /**
     * Performance metrics summary
     */
    class PerformanceSummary {
        private final long eventsSaved;
        private final long eventsLoaded;
        private final long aggregatesRehydrated;
        private final long commandsProcessed;
        private final double eventSaveRate;
        private final double eventLoadRate;
        private final double commandProcessingRate;
        private final BusinessMetrics businessMetrics;
        private final CacheMetrics cacheMetrics;
        
        public PerformanceSummary(long eventsSaved, long eventsLoaded, long aggregatesRehydrated, 
                                long commandsProcessed, double eventSaveRate, double eventLoadRate,
                                double commandProcessingRate, BusinessMetrics businessMetrics, 
                                CacheMetrics cacheMetrics) {
            this.eventsSaved = eventsSaved;
            this.eventsLoaded = eventsLoaded;
            this.aggregatesRehydrated = aggregatesRehydrated;
            this.commandsProcessed = commandsProcessed;
            this.eventSaveRate = eventSaveRate;
            this.eventLoadRate = eventLoadRate;
            this.commandProcessingRate = commandProcessingRate;
            this.businessMetrics = businessMetrics;
            this.cacheMetrics = cacheMetrics;
        }
        
        // Getters
        public long getEventsSaved() { return eventsSaved; }
        public long getEventsLoaded() { return eventsLoaded; }
        public long getAggregatesRehydrated() { return aggregatesRehydrated; }
        public long getCommandsProcessed() { return commandsProcessed; }
        public double getEventSaveRate() { return eventSaveRate; }
        public double getEventLoadRate() { return eventLoadRate; }
        public double getCommandProcessingRate() { return commandProcessingRate; }
        public BusinessMetrics getBusinessMetrics() { return businessMetrics; }
        public CacheMetrics getCacheMetrics() { return cacheMetrics; }
    }
    
    /**
     * Business operation metrics
     */
    class BusinessMetrics {
        private final long accountsCreated;
        private final long depositsProcessed;
        private final long withdrawalsProcessed;
        private final long transfersProcessed;
        private final long overdraftAttempts;
        
        public BusinessMetrics(long accountsCreated, long depositsProcessed, long withdrawalsProcessed,
                             long transfersProcessed, long overdraftAttempts) {
            this.accountsCreated = accountsCreated;
            this.depositsProcessed = depositsProcessed;
            this.withdrawalsProcessed = withdrawalsProcessed;
            this.transfersProcessed = transfersProcessed;
            this.overdraftAttempts = overdraftAttempts;
        }
        
        // Getters
        public long getAccountsCreated() { return accountsCreated; }
        public long getDepositsProcessed() { return depositsProcessed; }
        public long getWithdrawalsProcessed() { return withdrawalsProcessed; }
        public long getTransfersProcessed() { return transfersProcessed; }
        public long getOverdraftAttempts() { return overdraftAttempts; }
    }
    
    /**
     * Cache operation metrics
     */
    class CacheMetrics {
        private final long totalHits;
        private final long totalMisses;
        private final long totalEvictions;
        private final double overallHitRate;
        private final CacheTypeMetrics accountSummaryMetrics;
        private final CacheTypeMetrics balanceMetrics;
        private final CacheTypeMetrics accountsByHolderMetrics;
        private final CacheTypeMetrics eventCountMetrics;
        
        public CacheMetrics(long totalHits, long totalMisses, long totalEvictions, double overallHitRate,
                          CacheTypeMetrics accountSummaryMetrics, CacheTypeMetrics balanceMetrics,
                          CacheTypeMetrics accountsByHolderMetrics, CacheTypeMetrics eventCountMetrics) {
            this.totalHits = totalHits;
            this.totalMisses = totalMisses;
            this.totalEvictions = totalEvictions;
            this.overallHitRate = overallHitRate;
            this.accountSummaryMetrics = accountSummaryMetrics;
            this.balanceMetrics = balanceMetrics;
            this.accountsByHolderMetrics = accountsByHolderMetrics;
            this.eventCountMetrics = eventCountMetrics;
        }
        
        // Getters
        public long getTotalHits() { return totalHits; }
        public long getTotalMisses() { return totalMisses; }
        public long getTotalEvictions() { return totalEvictions; }
        public double getOverallHitRate() { return overallHitRate; }
        public CacheTypeMetrics getAccountSummaryMetrics() { return accountSummaryMetrics; }
        public CacheTypeMetrics getBalanceMetrics() { return balanceMetrics; }
        public CacheTypeMetrics getAccountsByHolderMetrics() { return accountsByHolderMetrics; }
        public CacheTypeMetrics getEventCountMetrics() { return eventCountMetrics; }
    }
    
    /**
     * Metrics for a specific cache type
     */
    class CacheTypeMetrics {
        private final String cacheType;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final double hitRate;
        
        public CacheTypeMetrics(String cacheType, long hits, long misses, long evictions, double hitRate) {
            this.cacheType = cacheType;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
        }
        
        // Getters
        public String getCacheType() { return cacheType; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public double getHitRate() { return hitRate; }
    }
}