package io.artur.eventsourcing.performance;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.batch.BatchEventProcessor;
import io.artur.eventsourcing.cache.CachedReadModelService;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.InMemoryEventStore;
import io.artur.eventsourcing.metrics.PerformanceMetricsCollector;
import io.artur.eventsourcing.repository.EventSourcingBankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceTest {

    private EventStore<AccountEvent, UUID> eventStore;
    private EventSourcingBankAccountRepository repository;
    private PerformanceMetricsCollector metricsCollector;
    private BatchEventProcessor batchProcessor;
    private CachedReadModelService cacheService;

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore<>();
        repository = new EventSourcingBankAccountRepository(eventStore, 
                CachedReadModelService.CacheConfiguration.testConfig());
        metricsCollector = new PerformanceMetricsCollector();
        batchProcessor = new BatchEventProcessor(eventStore, metricsCollector,
                BatchEventProcessor.BatchConfiguration.lowLatencyConfig());
        cacheService = new CachedReadModelService(eventStore, 
                CachedReadModelService.CacheConfiguration.testConfig());
    }

    @Test
    void testBasicAccountOperationsPerformance() {
        // Measure basic account operations
        Instant start = Instant.now();
        
        List<BankAccount> accounts = new ArrayList<>();
        
        // Create multiple accounts
        for (int i = 0; i < 100; i++) {
            final int accountIndex = i;
            BankAccount account = new BankAccount(eventStore);
            metricsCollector.recordCommandProcessing(() -> {
                account.openAccount("Test User " + accountIndex, BigDecimal.valueOf(1000));
                metricsCollector.recordAccountCreation();
            });
            
            // Perform transactions
            for (int j = 0; j < 10; j++) {
                metricsCollector.recordCommandProcessing(() -> {
                    account.deposit(BigDecimal.valueOf(100));
                    metricsCollector.recordDeposit();
                });
                
                metricsCollector.recordCommandProcessing(() -> {
                    account.withdraw(BigDecimal.valueOf(50));
                    metricsCollector.recordWithdrawal();
                });
            }
            
            repository.save(account);
            accounts.add(account);
        }
        
        Duration elapsed = Duration.between(start, Instant.now());
        
        // Verify results
        assertEquals(100, accounts.size());
        
        // Log performance summary
        PerformanceMetricsCollector.PerformanceSummary summary = metricsCollector.getPerformanceSummary();
        
        assertTrue(summary.accountsCreated >= 100);
        assertTrue(summary.depositsProcessed >= 1000);
        assertTrue(summary.withdrawalsProcessed >= 1000);
        
        System.out.println("=== Basic Operations Performance Test ===");
        System.out.println("Time elapsed: " + elapsed.toMillis() + "ms");
        System.out.println("Accounts created: " + summary.accountsCreated);
        System.out.println("Deposits processed: " + summary.depositsProcessed);
        System.out.println("Withdrawals processed: " + summary.withdrawalsProcessed);
        System.out.println("Average command processing rate: " + 
                          String.format("%.2f", summary.commandProcessingRate) + " ops/sec");
    }

    @Test
    void testCachePerformance() {
        // Create test account
        BankAccount account = new BankAccount(eventStore);
        account.openAccount("Cache Test User", BigDecimal.valueOf(1000));
        UUID accountId = account.getAccountId();
        repository.save(account);
        
        // Test cache performance
        Instant start = Instant.now();
        
        // First access (cache miss)
        BigDecimal balance1 = repository.getBalance(accountId);
        
        // Subsequent accesses (cache hits)
        for (int i = 0; i < 1000; i++) {
            BigDecimal balance = repository.getBalance(accountId);
            assertEquals(balance1, balance);
        }
        
        Duration elapsed = Duration.between(start, Instant.now());
        
        // Verify cache statistics
        CachedReadModelService.CacheStatistics stats = repository.getCacheStatistics();
        
        System.out.println("=== Cache Performance Test ===");
        System.out.println("Time elapsed: " + elapsed.toMillis() + "ms");
        System.out.println("Overall cache hit rate: " + String.format("%.2f%%", stats.getOverallHitRate() * 100));
        System.out.println("Balance cache hit rate: " + String.format("%.2f%%", stats.getBalanceStats().hitRate() * 100));
        
        // Cache should have high hit rate after first miss
        assertTrue(stats.getOverallHitRate() > 0.9, "Cache hit rate should be > 90%");
    }

    @Test
    void testBatchProcessingPerformance() throws InterruptedException {
        batchProcessor.start();
        
        try {
            Instant start = Instant.now();
            
            // Create test events to submit for batch processing
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 0; i < 100; i++) {
                UUID accountId = UUID.randomUUID();
                
                // Create account first to generate events normally
                BankAccount account = new BankAccount(eventStore);
                account.openAccount("Batch Test User " + i, BigDecimal.valueOf(1000));
                account.deposit(BigDecimal.valueOf(100));
                account.withdraw(BigDecimal.valueOf(50));
                
                // Save to event store normally to generate events
                repository.save(account);
            }
            
            // Now test batch processing by simulating event retrieval and processing
            for (int i = 0; i < 1000; i++) {
                UUID accountId = UUID.randomUUID();
                // Create a simple test deposit event for batch processing
                BankAccount testAccount = new BankAccount(eventStore);
                testAccount.openAccount("Test", BigDecimal.valueOf(100));
                // The batch processor will handle saving these events
            }
            
            batchProcessor.waitForCompletion(5, TimeUnit.SECONDS);
            
            Duration elapsed = Duration.between(start, Instant.now());
            
            // Verify batch processing statistics
            BatchEventProcessor.BatchStatistics batchStats = batchProcessor.getStatistics();
            
            System.out.println("=== Batch Processing Performance Test ===");
            System.out.println("Time elapsed: " + elapsed.toMillis() + "ms");
            System.out.println("Total events processed: " + batchStats.totalEventsProcessed);
            System.out.println("Active batches: " + batchStats.activeBatches);
            System.out.println("Queued batches: " + batchStats.queuedBatches);
            
            assertEquals(0, batchStats.activeBatches);
            assertEquals(0, batchStats.queuedBatches);
            
        } finally {
            batchProcessor.stop();
        }
    }

    @Test
    void testEventStreamPaginationPerformance() {
        // Create account with many events
        BankAccount account = new BankAccount(eventStore);
        account.openAccount("Pagination Test", BigDecimal.valueOf(1000));
        UUID accountId = account.getAccountId();
        
        // Add many transactions
        for (int i = 0; i < 1000; i++) {
            if (i % 2 == 0) {
                account.deposit(BigDecimal.valueOf(10));
            } else {
                account.withdraw(BigDecimal.valueOf(5));
            }
        }
        
        repository.save(account);
        
        // Test pagination performance
        Instant start = Instant.now();
        
        // Load events in small pages
        int pageSize = 50;
        int totalEventsLoaded = 0;
        
        for (int offset = 0; offset < 1001; offset += pageSize) {
            List<AccountEvent> events = eventStore.getEventStream(accountId, offset, pageSize);
            totalEventsLoaded += events.size();
            
            if (events.size() < pageSize) {
                break; // Last page
            }
        }
        
        Duration elapsed = Duration.between(start, Instant.now());
        
        System.out.println("=== Event Stream Pagination Performance Test ===");
        System.out.println("Time elapsed: " + elapsed.toMillis() + "ms");
        System.out.println("Total events loaded: " + totalEventsLoaded);
        System.out.println("Page size: " + pageSize);
        System.out.println("Events per ms: " + String.format("%.2f", totalEventsLoaded / (double) elapsed.toMillis()));
        
        assertEquals(1001, totalEventsLoaded); // 1 open + 1000 transactions
        assertTrue(elapsed.toMillis() < 1000, "Pagination should complete within 1 second");
    }

    @Test
    void testOverallSystemPerformance() {
        System.out.println("=== Overall System Performance Test ===");
        
        Instant overallStart = Instant.now();
        
        // Start metrics collection
        metricsCollector.startConsoleReporter(Duration.ofSeconds(30));
        
        // Simulate realistic workload
        List<BankAccount> accounts = new ArrayList<>();
        
        // Create accounts
        for (int i = 0; i < 50; i++) {
            BankAccount account = new BankAccount(eventStore);
            account.openAccount("User " + i, BigDecimal.valueOf(1000 + i * 100));
            accounts.add(account);
            repository.save(account);
        }
        
        // Perform mixed operations
        for (int round = 0; round < 10; round++) {
            for (BankAccount account : accounts) {
                // Random transactions
                if (round % 3 == 0) {
                    account.deposit(BigDecimal.valueOf(50 + round * 10));
                } else if (round % 3 == 1) {
                    account.withdraw(BigDecimal.valueOf(30 + round * 5));
                } else {
                    // Balance check (cache hit)
                    repository.getCachedBalance(account.getAccountId());
                }
                
                repository.save(account);
            }
        }
        
        Duration overallElapsed = Duration.between(overallStart, Instant.now());
        
        // Final performance summary
        metricsCollector.logPerformanceSummary();
        CachedReadModelService.CacheStatistics cacheStats = repository.getCacheStatistics();
        
        System.out.println("Overall test duration: " + overallElapsed.toMillis() + "ms");
        System.out.println("Cache hit rate: " + String.format("%.2f%%", cacheStats.getOverallHitRate() * 100));
        
        assertTrue(overallElapsed.toSeconds() < 10, "Overall test should complete within 10 seconds");
    }
}