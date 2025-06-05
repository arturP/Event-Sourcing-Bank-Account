package io.artur.eventsourcing.async;

import io.artur.eventsourcing.events.AccountEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AsyncEventProcessor {
    
    private static final Logger LOGGER = Logger.getLogger(AsyncEventProcessor.class.getName());
    
    private final ExecutorService eventProcessorExecutor;
    private final ExecutorService projectionExecutor;
    private final ExecutorService notificationExecutor;
    private volatile boolean isRunning = true;
    
    public AsyncEventProcessor() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    public AsyncEventProcessor(int numberOfThreads) {
        this.eventProcessorExecutor = Executors.newFixedThreadPool(numberOfThreads, 
                new NamedThreadFactory("EventProcessor"));
        this.projectionExecutor = Executors.newFixedThreadPool(Math.max(2, numberOfThreads / 2), 
                new NamedThreadFactory("ProjectionProcessor"));
        this.notificationExecutor = Executors.newFixedThreadPool(Math.max(1, numberOfThreads / 4), 
                new NamedThreadFactory("NotificationProcessor"));
        
        LOGGER.info(String.format("AsyncEventProcessor started with %d event threads, %d projection threads, %d notification threads", 
                numberOfThreads, Math.max(2, numberOfThreads / 2), Math.max(1, numberOfThreads / 4)));
    }
    
    public CompletableFuture<Void> processEventAsync(AccountEvent event, EventProcessor processor) {
        if (!isRunning) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncEventProcessor is shutdown"));
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.fine("Processing event asynchronously: " + event.getClass().getSimpleName());
                processor.process(event);
                LOGGER.fine("Completed processing event: " + event.getClass().getSimpleName());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing event: " + event.getClass().getSimpleName(), e);
                throw new RuntimeException("Event processing failed", e);
            }
        }, eventProcessorExecutor);
    }
    
    public CompletableFuture<Void> updateProjectionAsync(AccountEvent event, ProjectionUpdater updater) {
        if (!isRunning) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncEventProcessor is shutdown"));
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.fine("Updating projection for event: " + event.getClass().getSimpleName());
                updater.updateProjection(event);
                LOGGER.fine("Completed projection update for event: " + event.getClass().getSimpleName());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error updating projection for event: " + event.getClass().getSimpleName(), e);
                throw new RuntimeException("Projection update failed", e);
            }
        }, projectionExecutor);
    }
    
    public CompletableFuture<Void> sendNotificationAsync(AccountEvent event, NotificationSender sender) {
        if (!isRunning) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncEventProcessor is shutdown"));
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.fine("Sending notification for event: " + event.getClass().getSimpleName());
                sender.sendNotification(event);
                LOGGER.fine("Completed notification for event: " + event.getClass().getSimpleName());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error sending notification for event: " + event.getClass().getSimpleName(), e);
                // Don't throw exception for notifications as they are not critical
            }
        }, notificationExecutor);
    }
    
    public CompletableFuture<Void> processEventWithAllHandlers(AccountEvent event, 
                                                               EventProcessor processor,
                                                               ProjectionUpdater projectionUpdater,
                                                               NotificationSender notificationSender) {
        CompletableFuture<Void> eventProcessing = processEventAsync(event, processor);
        CompletableFuture<Void> projectionUpdate = updateProjectionAsync(event, projectionUpdater);
        CompletableFuture<Void> notification = sendNotificationAsync(event, notificationSender);
        
        return CompletableFuture.allOf(eventProcessing, projectionUpdate, notification);
    }
    
    public CompletableFuture<Void> processEventStreamAsync(List<AccountEvent> events, EventProcessor processor) {
        if (!isRunning) {
            return CompletableFuture.failedFuture(new IllegalStateException("AsyncEventProcessor is shutdown"));
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.fine("Processing event stream asynchronously: " + events.size() + " events");
                for (AccountEvent event : events) {
                    processor.process(event);
                }
                LOGGER.fine("Completed processing event stream: " + events.size() + " events");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing event stream", e);
                throw new RuntimeException("Event stream processing failed", e);
            }
        }, eventProcessorExecutor);
    }
    
    public CompletableFuture<Void> processEventStreamWithAllHandlers(List<AccountEvent> events,
                                                                     EventProcessor processor,
                                                                     ProjectionUpdater projectionUpdater,
                                                                     NotificationSender notificationSender) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (AccountEvent event : events) {
            futures.add(processEventWithAllHandlers(event, processor, projectionUpdater, notificationSender));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    public void shutdown() {
        LOGGER.info("Shutting down AsyncEventProcessor");
        isRunning = false;
        
        eventProcessorExecutor.shutdown();
        projectionExecutor.shutdown();
        notificationExecutor.shutdown();
        
        try {
            if (!eventProcessorExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                eventProcessorExecutor.shutdownNow();
            }
            if (!projectionExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                projectionExecutor.shutdownNow();
            }
            if (!notificationExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                notificationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Interrupted while waiting for executor shutdown");
        }
        
        LOGGER.info("AsyncEventProcessor shutdown completed");
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    @FunctionalInterface
    public interface EventProcessor {
        void process(AccountEvent event);
    }
    
    @FunctionalInterface
    public interface ProjectionUpdater {
        void updateProjection(AccountEvent event);
    }
    
    @FunctionalInterface
    public interface NotificationSender {
        void sendNotification(AccountEvent event);
    }
    
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix + "-";
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}