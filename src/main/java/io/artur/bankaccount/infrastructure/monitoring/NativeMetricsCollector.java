package io.artur.bankaccount.infrastructure.monitoring;

import io.artur.bankaccount.application.ports.outgoing.MetricsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Native metrics collector implementation that directly implements MetricsPort
 * without depending on legacy infrastructure
 */
@Component
public class NativeMetricsCollector implements MetricsPort {
    
    private static final Logger logger = LoggerFactory.getLogger(NativeMetricsCollector.class);
    
    // Business metrics
    private final AtomicLong accountsCreated = new AtomicLong(0);
    private final AtomicLong depositsProcessed = new AtomicLong(0);
    private final AtomicLong withdrawalsProcessed = new AtomicLong(0);
    private final AtomicLong transfersProcessed = new AtomicLong(0);
    private final AtomicLong overdraftAttempts = new AtomicLong(0);
    
    // Performance metrics
    private final AtomicLong eventsSaved = new AtomicLong(0);
    private final AtomicLong eventsLoaded = new AtomicLong(0);
    private final AtomicLong snapshotsSaved = new AtomicLong(0);
    private final AtomicLong snapshotsLoaded = new AtomicLong(0);
    private final AtomicLong commandsProcessed = new AtomicLong(0);
    private final AtomicLong aggregatesRehydrated = new AtomicLong(0);
    
    // Cache metrics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheEvictions = new AtomicLong(0);
    
    // Timing metrics
    private final Map<String, AtomicLong> timingCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> timingTotals = new ConcurrentHashMap<>();
    
    // Performance rates
    private final LocalDateTime startTime = LocalDateTime.now();
    
    // Periodic reporting
    private ScheduledExecutorService reportingScheduler;
    
    public NativeMetricsCollector() {
        logger.info("Native metrics collector initialized");
    }
    
    @Override
    public void recordEventBatchSize(int batchSize) {
        logger.debug("Event batch size recorded: {}", batchSize);
    }
    
    @Override
    public <T> T recordAggregateRehydration(Supplier<T> rehydrationOperation) {
        long startTime = System.nanoTime();
        try {
            T result = rehydrationOperation.get();
            aggregatesRehydrated.incrementAndGet();
            return result;
        } finally {
            long duration = System.nanoTime() - startTime;
            recordTiming("aggregate_rehydration", duration);
        }
    }
    
    @Override
    public void recordAggregateSnapshot(Runnable snapshotOperation) {
        long startTime = System.nanoTime();
        try {
            snapshotOperation.run();
            snapshotsSaved.incrementAndGet();
        } finally {
            long duration = System.nanoTime() - startTime;
            recordTiming("aggregate_snapshot", duration);
        }
    }
    
    @Override
    public void recordCustomMetric(String metricName, double value) {
        logger.debug("Custom metric recorded: {} = {}", metricName, value);
    }
    
    @Override
    public void recordCustomMetric(String metricName, long value) {
        logger.debug("Custom metric recorded: {} = {}", metricName, value);
    }
    
    @Override
    public void recordAccountCreation() {
        accountsCreated.incrementAndGet();
        logger.debug("Account creation recorded. Total: {}", accountsCreated.get());
    }
    
    @Override
    public void recordDeposit() {
        depositsProcessed.incrementAndGet();
        logger.debug("Deposit recorded. Total: {}", depositsProcessed.get());
    }
    
    @Override
    public void recordWithdrawal() {
        withdrawalsProcessed.incrementAndGet();
        logger.debug("Withdrawal recorded. Total: {}", withdrawalsProcessed.get());
    }
    
    @Override
    public void recordTransfer() {
        transfersProcessed.incrementAndGet();
        logger.debug("Transfer recorded. Total: {}", transfersProcessed.get());
    }
    
    @Override
    public void recordOverdraftAttempt() {
        overdraftAttempts.incrementAndGet();
        logger.debug("Overdraft attempt recorded. Total: {}", overdraftAttempts.get());
    }
    
    @Override
    public void recordAccountStatusChange(String newStatus) {
        logger.info("Account status changed to: {}", newStatus);
        // You could add specific metrics tracking here if needed
        recordCustomMetric("account_status_changes", 1);
    }
    
    @Override
    public void recordCacheHit(String cacheType) {
        cacheHits.incrementAndGet();
        logger.debug("Cache hit recorded for type: {}. Total hits: {}", cacheType, cacheHits.get());
    }
    
    @Override
    public void recordCacheMiss(String cacheType) {
        cacheMisses.incrementAndGet();
        logger.debug("Cache miss recorded for type: {}. Total misses: {}", cacheType, cacheMisses.get());
    }
    
    @Override
    public void recordCacheEviction(String cacheType) {
        cacheEvictions.incrementAndGet();
        logger.debug("Cache eviction recorded for type: {}. Total evictions: {}", cacheType, cacheEvictions.get());
    }
    
    @Override
    public <T> T recordCommandProcessing(Supplier<T> operation) {
        long startTime = System.nanoTime();
        try {
            T result = operation.get();
            commandsProcessed.incrementAndGet();
            return result;
        } finally {
            long duration = System.nanoTime() - startTime;
            recordTiming("command_processing", duration);
        }
    }
    
    @Override
    public void recordCommandProcessing(Runnable operation) {
        long startTime = System.nanoTime();
        try {
            operation.run();
            commandsProcessed.incrementAndGet();
        } finally {
            long duration = System.nanoTime() - startTime;
            recordTiming("command_processing", duration);
        }
    }
    
    @Override
    public TimingContext startTiming(String operationName) {
        return new NativeTimingContext(operationName);
    }
    
    @Override
    public void recordEventSave(Runnable operation) {
        long startTime = System.nanoTime();
        try {
            operation.run();
            eventsSaved.incrementAndGet();
        } finally {
            long duration = System.nanoTime() - startTime;
            recordTiming("event_save", duration);
        }
    }
    
    @Override
    public <T> T recordEventLoad(Supplier<T> operation) {
        long startTime = System.nanoTime();
        try {
            T result = operation.get();
            eventsLoaded.incrementAndGet();
            return result;
        } finally {
            long duration = System.nanoTime() - startTime;
            recordTiming("event_load", duration);
        }
    }
    
    @Override
    public PerformanceSummary getPerformanceSummary() {
        Duration uptime = Duration.between(startTime, LocalDateTime.now());
        double eventSaveRate = uptime.toMillis() > 0 ? (double) eventsSaved.get() / (uptime.toMillis() / 1000.0) : 0.0;
        double eventLoadRate = uptime.toMillis() > 0 ? (double) eventsLoaded.get() / (uptime.toMillis() / 1000.0) : 0.0;
        double commandRate = uptime.toMillis() > 0 ? (double) commandsProcessed.get() / (uptime.toMillis() / 1000.0) : 0.0;
        
        BusinessMetrics businessMetrics = new BusinessMetrics(
            accountsCreated.get(), depositsProcessed.get(), withdrawalsProcessed.get(),
            transfersProcessed.get(), overdraftAttempts.get()
        );
        
        double overallHitRate = (cacheHits.get() + cacheMisses.get()) > 0 ? 
            (double) cacheHits.get() / (cacheHits.get() + cacheMisses.get()) : 0.0;
        
        CacheMetrics cacheMetrics = new CacheMetrics(
            cacheHits.get(), cacheMisses.get(), cacheEvictions.get(), overallHitRate,
            new CacheTypeMetrics("account_summary", 0, 0, 0, 0.0),
            new CacheTypeMetrics("balance", 0, 0, 0, 0.0),
            new CacheTypeMetrics("accounts_by_holder", 0, 0, 0, 0.0),
            new CacheTypeMetrics("event_count", 0, 0, 0, 0.0)
        );
        
        return new PerformanceSummary(
            eventsSaved.get(), eventsLoaded.get(), 0L, commandsProcessed.get(),
            eventSaveRate, eventLoadRate, commandRate, businessMetrics, cacheMetrics
        );
    }
    
    @Override
    public void logPerformanceSummary() {
        logger.info("=== Native Performance Summary ===");
        logger.info("Events - Saved: {}, Loaded: {}", eventsSaved.get(), eventsLoaded.get());
        logger.info("Snapshots - Saved: {}, Loaded: {}", snapshotsSaved.get(), snapshotsLoaded.get());
        logger.info("Commands processed: {}", commandsProcessed.get());
        logger.info("Business - Accounts: {}, Deposits: {}, Withdrawals: {}, Transfers: {}", 
                   accountsCreated.get(), depositsProcessed.get(), withdrawalsProcessed.get(), transfersProcessed.get());
        logger.info("Cache - Hits: {}, Misses: {}, Evictions: {}", 
                   cacheHits.get(), cacheMisses.get(), cacheEvictions.get());
        logger.info("=== End Native Performance Summary ===");
    }
    
    @Override
    public void startPeriodicReporting(Duration period) {
        if (reportingScheduler != null) {
            reportingScheduler.shutdown();
        }
        
        reportingScheduler = Executors.newSingleThreadScheduledExecutor();
        reportingScheduler.scheduleAtFixedRate(
            this::logPerformanceSummary,
            period.toMillis(),
            period.toMillis(),
            TimeUnit.MILLISECONDS
        );
        
        logger.info("Periodic reporting started with period: {}", period);
    }
    
    @Override
    public void stopPeriodicReporting() {
        if (reportingScheduler != null) {
            reportingScheduler.shutdown();
            reportingScheduler = null;
            logger.info("Periodic reporting stopped");
        }
    }
    
    private void recordTiming(String operation, long durationNanos) {
        timingCounters.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
        timingTotals.computeIfAbsent(operation, k -> new AtomicLong(0)).addAndGet(durationNanos);
    }
    
    private class NativeTimingContext implements TimingContext {
        private final String operationName;
        private final long startTime;
        private final Map<String, String> tags = new ConcurrentHashMap<>();
        
        public NativeTimingContext(String operationName) {
            this.operationName = operationName;
            this.startTime = System.nanoTime();
        }
        
        @Override
        public void addTag(String key, String value) {
            tags.put(key, value);
        }
        
        @Override
        public Duration getElapsed() {
            return Duration.ofNanos(System.nanoTime() - startTime);
        }
        
        @Override
        public void close() {
            long duration = System.nanoTime() - startTime;
            recordTiming(operationName, duration);
            
            if (!tags.isEmpty()) {
                logger.debug("Timing tag for {}: {}", operationName, 
                           tags.entrySet().stream()
                               .map(e -> e.getKey() + "=" + e.getValue())
                               .reduce((a, b) -> a + ", " + b)
                               .orElse(""));
            }
        }
    }
    
    // Removed NativePerformanceSummary class - using MetricsPort.PerformanceSummary directly
        
}