package io.artur.eventsourcing;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.config.DatabaseConfig;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.EventMetadata;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.JdbcEventStore;
import io.artur.eventsourcing.exceptions.OverdraftExceededException;
import io.artur.eventsourcing.projections.AccountSummaryProjection;
import io.artur.eventsourcing.projections.AccountSummaryProjectionHandler;
import io.artur.eventsourcing.replay.EventReplayService;
import io.artur.eventsourcing.snapshots.JdbcSnapshotStore;
import io.artur.eventsourcing.snapshots.SnapshotStore;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class Main {
    
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    public static void main(String[] args) {
        // Initialize database configuration
        DatabaseConfig dbConfig = new DatabaseConfig();
        
        // Create snapshot store
        SnapshotStore snapshotStore = new JdbcSnapshotStore(dbConfig.getDataSource());
        
        // Create event store with dependency injection
        EventStore<AccountEvent, UUID> eventStore = new JdbcEventStore(
                dbConfig.getDataSource(), snapshotStore);
        
        // Create a new account with overdraft protection and enhanced metadata
        BankAccount account = new BankAccount(eventStore);
        EventMetadata metadata = new EventMetadata("correlation-123", null, "user-456", 
                "Bank-App/1.0", "192.168.1.100", 1, Map.of("source", "main-demo"));
        account.openAccount("John Doe", BigDecimal.valueOf(200.00), metadata);
        UUID accountId = account.getAccountId();
        
        LOGGER.info("Created new account with ID: " + accountId + " and overdraft limit: $" + account.getOverdraftLimit());
        
        // Perform some operations
        account.deposit(BigDecimal.valueOf(500.00));
        LOGGER.info("Deposited $500.00, new balance: " + account.getBalance());
        
        account.withdraw(BigDecimal.valueOf(150.00));
        LOGGER.info("Withdrew $150.00, new balance: " + account.getBalance());
        
        // Test overdraft functionality
        account.withdraw(BigDecimal.valueOf(400.00));
        LOGGER.info("Withdrew $400.00 (using overdraft), new balance: " + account.getBalance());
        
        // Try to exceed overdraft limit
        try {
            account.withdraw(BigDecimal.valueOf(200.00));
        } catch (OverdraftExceededException e) {
            LOGGER.warning("Overdraft limit exceeded: " + e.getMessage());
        }
        
        // Create many events to trigger snapshot creation
        for (int i = 0; i < 9; i++) {
            account.deposit(BigDecimal.valueOf(10.00));
        }
        LOGGER.info("Made additional deposits, final balance: " + account.getBalance());
        
        // Demonstrate event replay service
        EventReplayService replayService = new EventReplayService(eventStore);
        LOGGER.info("=== Event Replay Demo ===");
        replayService.debugEventStream(accountId);
        
        // Demonstrate projection handler
        AccountSummaryProjectionHandler projectionHandler = new AccountSummaryProjectionHandler();
        replayService.replayEvents(accountId, projectionHandler::handle);
        AccountSummaryProjection projection = projectionHandler.getProjection();
        
        LOGGER.info("=== Account Summary Projection ===");
        LOGGER.info("Account holder: " + projection.getAccountHolder());
        LOGGER.info("Current balance: " + projection.getCurrentBalance());
        LOGGER.info("Transaction count: " + projection.getTransactionCount());
        LOGGER.info("Last transaction: " + projection.getLastTransactionDate());
        
        // Load the account from the event store
        BankAccount loadedAccount = BankAccount.loadFromStore(eventStore, accountId);
        LOGGER.info("Loaded account from store. Account holder: " + loadedAccount.getAccountHolder() + 
                ", Balance: " + loadedAccount.getBalance() + ", Overdraft limit: $" + loadedAccount.getOverdraftLimit());
    }
}