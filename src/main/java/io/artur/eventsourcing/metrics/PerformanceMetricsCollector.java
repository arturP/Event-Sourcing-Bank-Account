package io.artur.eventsourcing.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class PerformanceMetricsCollector {
    
    private static final Logger LOGGER = Logger.getLogger(PerformanceMetricsCollector.class.getName());
    
    private final MetricRegistry metricRegistry;
    private final AtomicLong eventsSaved = new AtomicLong(0);
    private final AtomicLong eventsLoaded = new AtomicLong(0);
    private final AtomicLong snapshotsSaved = new AtomicLong(0);
    private final AtomicLong snapshotsLoaded = new AtomicLong(0);
    
    // Timers for operation performance tracking
    private final Timer eventSaveTimer;
    private final Timer eventLoadTimer;
    private final Timer snapshotSaveTimer;
    private final Timer snapshotLoadTimer;
    private final Timer aggregateRehydrationTimer;
    private final Timer commandProcessingTimer;
    
    // Counters for business metrics
    private final Counter accountsCreated;
    private final Counter depositsProcessed;
    private final Counter withdrawalsProcessed;
    private final Counter overdraftAttempts;
    
    // Gauges for system state
    private final Gauge<Long> totalEvents;
    private final Gauge<Long> totalSnapshots;
    private final Histogram eventBatchSizes;
    
    public PerformanceMetricsCollector() {
        this.metricRegistry = new MetricRegistry();
        
        // Register JVM metrics
        metricRegistry.registerAll(new GarbageCollectorMetricSet());
        metricRegistry.registerAll(new MemoryUsageGaugeSet());
        metricRegistry.registerAll(new ThreadStatesGaugeSet());
        
        // Initialize timers
        this.eventSaveTimer = metricRegistry.timer(MetricRegistry.name("events", "save", "time"));
        this.eventLoadTimer = metricRegistry.timer(MetricRegistry.name("events", "load", "time"));
        this.snapshotSaveTimer = metricRegistry.timer(MetricRegistry.name("snapshots", "save", "time"));
        this.snapshotLoadTimer = metricRegistry.timer(MetricRegistry.name("snapshots", "load", "time"));
        this.aggregateRehydrationTimer = metricRegistry.timer(MetricRegistry.name("aggregates", "rehydration", "time"));
        this.commandProcessingTimer = metricRegistry.timer(MetricRegistry.name("commands", "processing", "time"));
        
        // Initialize counters
        this.accountsCreated = metricRegistry.counter(MetricRegistry.name("business", "accounts", "created"));
        this.depositsProcessed = metricRegistry.counter(MetricRegistry.name("business", "deposits", "processed"));
        this.withdrawalsProcessed = metricRegistry.counter(MetricRegistry.name("business", "withdrawals", "processed"));
        this.overdraftAttempts = metricRegistry.counter(MetricRegistry.name("business", "overdraft", "attempts"));
        
        // Initialize gauges
        this.totalEvents = metricRegistry.gauge(MetricRegistry.name("events", "total"), () -> eventsSaved::get);
        this.totalSnapshots = metricRegistry.gauge(MetricRegistry.name("snapshots", "total"), () -> snapshotsSaved::get);
        
        // Initialize histograms
        this.eventBatchSizes = metricRegistry.histogram(MetricRegistry.name("events", "batch", "sizes"));
        
        LOGGER.info("Performance metrics collector initialized");
    }
    
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }
    
    // Event Store Metrics
    public void recordEventSave(Runnable saveOperation) {
        try (Timer.Context context = eventSaveTimer.time()) {
            saveOperation.run();
            eventsSaved.incrementAndGet();
        }
    }
    
    public <T> T recordEventLoad(Supplier<T> loadOperation) {
        try (Timer.Context context = eventLoadTimer.time()) {
            T result = loadOperation.get();
            eventsLoaded.incrementAndGet();
            return result;
        }
    }
    
    public void recordEventBatchSize(int batchSize) {
        eventBatchSizes.update(batchSize);
    }
    
    // Snapshot Metrics
    public void recordSnapshotSave(Runnable saveOperation) {
        try (Timer.Context context = snapshotSaveTimer.time()) {
            saveOperation.run();
            snapshotsSaved.incrementAndGet();
        }
    }
    
    public <T> T recordSnapshotLoad(Supplier<T> loadOperation) {
        try (Timer.Context context = snapshotLoadTimer.time()) {
            T result = loadOperation.get();
            snapshotsLoaded.incrementAndGet();
            return result;
        }
    }
    
    // Aggregate Metrics
    public <T> T recordAggregateRehydration(Supplier<T> rehydrationOperation) {
        try (Timer.Context context = aggregateRehydrationTimer.time()) {
            return rehydrationOperation.get();
        }
    }
    
    // Command Processing Metrics
    public void recordCommandProcessing(Runnable commandOperation) {
        try (Timer.Context context = commandProcessingTimer.time()) {
            commandOperation.run();
        }
    }
    
    public <T> T recordCommandProcessing(Supplier<T> commandOperation) {
        try (Timer.Context context = commandProcessingTimer.time()) {
            return commandOperation.get();
        }
    }
    
    // Business Metrics
    public void recordAccountCreation() {
        accountsCreated.inc();
    }
    
    public void recordDeposit() {
        depositsProcessed.inc();
    }
    
    public void recordWithdrawal() {
        withdrawalsProcessed.inc();
    }
    
    public void recordOverdraftAttempt() {
        overdraftAttempts.inc();
    }
    
    // Custom timing for operations
    public TimingContext startTiming(String operation) {
        return new TimingContext(operation, Instant.now());
    }
    
    // Performance summary
    public PerformanceSummary getPerformanceSummary() {
        return new PerformanceSummary(
            eventsSaved.get(),
            eventsLoaded.get(),
            snapshotsSaved.get(),
            snapshotsLoaded.get(),
            eventSaveTimer.getMeanRate(),
            eventLoadTimer.getMeanRate(),
            aggregateRehydrationTimer.getMeanRate(),
            commandProcessingTimer.getMeanRate(),
            accountsCreated.getCount(),
            depositsProcessed.getCount(),
            withdrawalsProcessed.getCount(),
            overdraftAttempts.getCount()
        );
    }
    
    // Console reporter for development
    public void startConsoleReporter(Duration period) {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(period.toSeconds(), TimeUnit.SECONDS);
        LOGGER.info("Console metrics reporter started with period: " + period);
    }
    
    public void logPerformanceSummary() {
        PerformanceSummary summary = getPerformanceSummary();
        LOGGER.info("=== Performance Summary ===");
        LOGGER.info("Events - Saved: " + summary.eventsSaved + ", Loaded: " + summary.eventsLoaded);
        LOGGER.info("Snapshots - Saved: " + summary.snapshotsSaved + ", Loaded: " + summary.snapshotsLoaded);
        LOGGER.info("Rates (ops/sec) - Event Save: " + String.format("%.2f", summary.eventSaveRate) + 
                   ", Event Load: " + String.format("%.2f", summary.eventLoadRate));
        LOGGER.info("Business - Accounts: " + summary.accountsCreated + 
                   ", Deposits: " + summary.depositsProcessed + 
                   ", Withdrawals: " + summary.withdrawalsProcessed);
        LOGGER.info("=== End Performance Summary ===");
    }
    
    public static class TimingContext implements AutoCloseable {
        private final String operation;
        private final Instant startTime;
        
        public TimingContext(String operation, Instant startTime) {
            this.operation = operation;
            this.startTime = startTime;
        }
        
        @Override
        public void close() {
            Duration elapsed = Duration.between(startTime, Instant.now());
            LOGGER.fine("Operation '" + operation + "' completed in " + elapsed.toMillis() + "ms");
        }
        
        public Duration getElapsed() {
            return Duration.between(startTime, Instant.now());
        }
    }
    
    public static class PerformanceSummary {
        public final long eventsSaved;
        public final long eventsLoaded;
        public final long snapshotsSaved;
        public final long snapshotsLoaded;
        public final double eventSaveRate;
        public final double eventLoadRate;
        public final double aggregateRehydrationRate;
        public final double commandProcessingRate;
        public final long accountsCreated;
        public final long depositsProcessed;
        public final long withdrawalsProcessed;
        public final long overdraftAttempts;
        
        public PerformanceSummary(long eventsSaved, long eventsLoaded, long snapshotsSaved, 
                                long snapshotsLoaded, double eventSaveRate, double eventLoadRate,
                                double aggregateRehydrationRate, double commandProcessingRate,
                                long accountsCreated, long depositsProcessed, 
                                long withdrawalsProcessed, long overdraftAttempts) {
            this.eventsSaved = eventsSaved;
            this.eventsLoaded = eventsLoaded;
            this.snapshotsSaved = snapshotsSaved;
            this.snapshotsLoaded = snapshotsLoaded;
            this.eventSaveRate = eventSaveRate;
            this.eventLoadRate = eventLoadRate;
            this.aggregateRehydrationRate = aggregateRehydrationRate;
            this.commandProcessingRate = commandProcessingRate;
            this.accountsCreated = accountsCreated;
            this.depositsProcessed = depositsProcessed;
            this.withdrawalsProcessed = withdrawalsProcessed;
            this.overdraftAttempts = overdraftAttempts;
        }
    }
}