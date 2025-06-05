package io.artur.eventsourcing.batch;

import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.metrics.PerformanceMetricsCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BatchEventProcessor {
    
    private static final Logger LOGGER = Logger.getLogger(BatchEventProcessor.class.getName());
    
    private final EventStore<AccountEvent, UUID> eventStore;
    private final PerformanceMetricsCollector metricsCollector;
    private final BatchConfiguration config;
    private final BlockingQueue<EventBatch> batchQueue;
    private final ExecutorService batchProcessorExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicInteger activeBatches = new AtomicInteger(0);
    
    // Current batch being accumulated
    private volatile EventBatch currentBatch;
    private final Object batchLock = new Object();
    
    public BatchEventProcessor(EventStore<AccountEvent, UUID> eventStore, 
                             PerformanceMetricsCollector metricsCollector) {
        this(eventStore, metricsCollector, BatchConfiguration.defaultConfig());
    }
    
    public BatchEventProcessor(EventStore<AccountEvent, UUID> eventStore, 
                             PerformanceMetricsCollector metricsCollector,
                             BatchConfiguration config) {
        this.eventStore = eventStore;
        this.metricsCollector = metricsCollector;
        this.config = config;
        this.batchQueue = new LinkedBlockingQueue<>(config.getMaxQueueSize());
        this.batchProcessorExecutor = Executors.newFixedThreadPool(config.getProcessorThreads(),
                r -> new Thread(r, "BatchEventProcessor-" + UUID.randomUUID().toString().substring(0, 8)));
        this.scheduledExecutor = Executors.newScheduledThreadPool(1,
                r -> new Thread(r, "BatchEventScheduler"));
        this.currentBatch = new EventBatch();
        
        LOGGER.info("BatchEventProcessor initialized with config: " + config);
    }
    
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            // Start batch processors
            for (int i = 0; i < config.getProcessorThreads(); i++) {
                batchProcessorExecutor.submit(this::processBatches);
            }
            
            // Start batch flush scheduler
            scheduledExecutor.scheduleAtFixedRate(this::flushCurrentBatch, 
                    config.getFlushIntervalMs(), config.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
            
            LOGGER.info("BatchEventProcessor started with " + config.getProcessorThreads() + " threads");
        }
    }
    
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            LOGGER.info("Stopping BatchEventProcessor...");
            
            // Flush any remaining events
            flushCurrentBatch();
            
            // Shutdown executors
            scheduledExecutor.shutdown();
            batchProcessorExecutor.shutdown();
            
            try {
                if (!batchProcessorExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    batchProcessorExecutor.shutdownNow();
                }
                if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warning("Interrupted while waiting for batch processor shutdown");
            }
            
            LOGGER.info("BatchEventProcessor stopped. Total events processed: " + totalEventsProcessed.get());
        }
    }
    
    public void submitEvent(UUID accountId, AccountEvent event) {
        submitEvent(accountId, event, null);
    }
    
    public void submitEvent(UUID accountId, AccountEvent event, Consumer<Exception> errorHandler) {
        if (!isRunning.get()) {
            throw new IllegalStateException("BatchEventProcessor is not running");
        }
        
        synchronized (batchLock) {
            currentBatch.addEvent(accountId, event, errorHandler);
            
            if (currentBatch.size() >= config.getBatchSize()) {
                flushCurrentBatchUnsafe();
            }
        }
    }
    
    public CompletableFuture<Void> submitEventAsync(UUID accountId, AccountEvent event) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        submitEvent(accountId, event, exception -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                future.complete(null);
            }
        });
        
        return future;
    }
    
    private void flushCurrentBatch() {
        synchronized (batchLock) {
            flushCurrentBatchUnsafe();
        }
    }
    
    private void flushCurrentBatchUnsafe() {
        if (!currentBatch.isEmpty()) {
            EventBatch batchToProcess = currentBatch;
            currentBatch = new EventBatch();
            
            try {
                batchQueue.put(batchToProcess);
                activeBatches.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warning("Interrupted while queuing batch for processing");
                // Re-add events to current batch
                for (EventItem item : batchToProcess.getEvents()) {
                    currentBatch.addEvent(item.getAccountId(), item.getEvent(), item.getErrorHandler());
                }
            }
        }
    }
    
    private void processBatches() {
        while (isRunning.get() || !batchQueue.isEmpty()) {
            try {
                EventBatch batch = batchQueue.poll(1, TimeUnit.SECONDS);
                if (batch != null && !batch.isEmpty()) {
                    processBatch(batch);
                    activeBatches.decrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in batch processing thread", e);
            }
        }
    }
    
    private void processBatch(EventBatch batch) {
        LOGGER.fine("Processing batch with " + batch.size() + " events");
        
        try (PerformanceMetricsCollector.TimingContext timing = 
                 metricsCollector.startTiming("batch-processing")) {
            
            for (EventItem item : batch.getEvents()) {
                try {
                    metricsCollector.recordEventSave(() -> 
                        eventStore.saveEvent(item.getAccountId(), item.getEvent()));
                    
                    // Notify success
                    if (item.getErrorHandler() != null) {
                        item.getErrorHandler().accept(null);
                    }
                    
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to save event in batch: " + 
                              item.getEvent().getClass().getSimpleName(), e);
                    
                    // Notify error
                    if (item.getErrorHandler() != null) {
                        item.getErrorHandler().accept(e);
                    }
                }
            }
            
            totalEventsProcessed.addAndGet(batch.size());
            metricsCollector.recordEventBatchSize(batch.size());
            
            LOGGER.fine("Completed batch processing: " + batch.size() + " events in " + 
                       timing.getElapsed().toMillis() + "ms");
        }
    }
    
    public BatchStatistics getStatistics() {
        synchronized (batchLock) {
            return new BatchStatistics(
                totalEventsProcessed.get(),
                activeBatches.get(),
                batchQueue.size(),
                currentBatch.size(),
                isRunning.get()
            );
        }
    }
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
    public void waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        
        while (System.nanoTime() < deadline) {
            synchronized (batchLock) {
                if (currentBatch.isEmpty() && batchQueue.isEmpty() && activeBatches.get() == 0) {
                    return;
                }
            }
            Thread.sleep(100);
        }
        
        throw new InterruptedException("Timeout waiting for batch completion");
    }
    
    static class EventBatch {
        private final List<EventItem> events = new ArrayList<>();
        
        void addEvent(UUID accountId, AccountEvent event, Consumer<Exception> errorHandler) {
            events.add(new EventItem(accountId, event, errorHandler));
        }
        
        List<EventItem> getEvents() {
            return events;
        }
        
        int size() {
            return events.size();
        }
        
        boolean isEmpty() {
            return events.isEmpty();
        }
    }
    
    static class EventItem {
        private final UUID accountId;
        private final AccountEvent event;
        private final Consumer<Exception> errorHandler;
        
        EventItem(UUID accountId, AccountEvent event, Consumer<Exception> errorHandler) {
            this.accountId = accountId;
            this.event = event;
            this.errorHandler = errorHandler;
        }
        
        UUID getAccountId() { return accountId; }
        AccountEvent getEvent() { return event; }
        Consumer<Exception> getErrorHandler() { return errorHandler; }
    }
    
    public static class BatchConfiguration {
        private final int batchSize;
        private final long flushIntervalMs;
        private final int processorThreads;
        private final int maxQueueSize;
        
        public BatchConfiguration(int batchSize, long flushIntervalMs, int processorThreads, int maxQueueSize) {
            this.batchSize = batchSize;
            this.flushIntervalMs = flushIntervalMs;
            this.processorThreads = processorThreads;
            this.maxQueueSize = maxQueueSize;
        }
        
        public static BatchConfiguration defaultConfig() {
            return new BatchConfiguration(50, 1000, 2, 1000);
        }
        
        public static BatchConfiguration highThroughputConfig() {
            return new BatchConfiguration(100, 500, 4, 2000);
        }
        
        public static BatchConfiguration lowLatencyConfig() {
            return new BatchConfiguration(10, 100, 1, 500);
        }
        
        public int getBatchSize() { return batchSize; }
        public long getFlushIntervalMs() { return flushIntervalMs; }
        public int getProcessorThreads() { return processorThreads; }
        public int getMaxQueueSize() { return maxQueueSize; }
        
        @Override
        public String toString() {
            return String.format("BatchConfig{batchSize=%d, flushMs=%d, threads=%d, queueSize=%d}", 
                               batchSize, flushIntervalMs, processorThreads, maxQueueSize);
        }
    }
    
    public static class BatchStatistics {
        public final long totalEventsProcessed;
        public final int activeBatches;
        public final int queuedBatches;
        public final int currentBatchSize;
        public final boolean isRunning;
        
        public BatchStatistics(long totalEventsProcessed, int activeBatches, int queuedBatches, 
                             int currentBatchSize, boolean isRunning) {
            this.totalEventsProcessed = totalEventsProcessed;
            this.activeBatches = activeBatches;
            this.queuedBatches = queuedBatches;
            this.currentBatchSize = currentBatchSize;
            this.isRunning = isRunning;
        }
        
        @Override
        public String toString() {
            return String.format("BatchStats{processed=%d, active=%d, queued=%d, current=%d, running=%s}", 
                               totalEventsProcessed, activeBatches, queuedBatches, currentBatchSize, isRunning);
        }
    }
}