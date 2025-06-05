package io.artur.eventsourcing;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.config.DatabaseConfig;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.JdbcEventStore;
import io.artur.eventsourcing.snapshots.JdbcSnapshotStore;
import io.artur.eventsourcing.snapshots.SnapshotStore;

import java.math.BigDecimal;
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
        
        // Create a new account
        BankAccount account = new BankAccount(eventStore);
        account.openAccount("John Doe");
        UUID accountId = account.getAccountId();
        
        LOGGER.info("Created new account with ID: " + accountId);
        
        // Perform some operations
        account.deposit(BigDecimal.valueOf(500.00));
        LOGGER.info("Deposited $500.00, new balance: " + account.getBalance());
        
        account.withdraw(BigDecimal.valueOf(150.00));
        LOGGER.info("Withdrew $150.00, new balance: " + account.getBalance());
        
        // Create many events to trigger snapshot creation
        for (int i = 0; i < 9; i++) {
            account.deposit(BigDecimal.valueOf(10.00));
        }
        LOGGER.info("Made additional deposits, final balance: " + account.getBalance());
        
        // Load the account from the event store
        BankAccount loadedAccount = BankAccount.loadFromStore(eventStore, accountId);
        LOGGER.info("Loaded account from store. Account holder: " + loadedAccount.getAccountHolder() + 
                ", Balance: " + loadedAccount.getBalance());
    }
}