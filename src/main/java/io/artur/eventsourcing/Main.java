package io.artur.eventsourcing;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.config.DatabaseConfig;
import io.artur.eventsourcing.cqrs.handlers.GetAccountBalanceQueryHandler;
import io.artur.eventsourcing.cqrs.handlers.OpenAccountCommandHandler;
import io.artur.eventsourcing.cqrs.queries.GetAccountBalanceQuery;
import io.artur.eventsourcing.domain.Money;
import io.artur.eventsourcing.domain.services.AccountValidationService;
import io.artur.eventsourcing.domain.services.OverdraftService;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.EventMetadata;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.JdbcEventStore;
import io.artur.eventsourcing.exceptions.OverdraftExceededException;
import io.artur.eventsourcing.projections.AccountSummaryProjection;
import io.artur.eventsourcing.projections.AccountSummaryProjectionHandler;
import io.artur.eventsourcing.replay.EventReplayService;
import io.artur.eventsourcing.repository.BankAccountRepository;
import io.artur.eventsourcing.repository.EventSourcingBankAccountRepository;
import io.artur.eventsourcing.snapshots.JdbcSnapshotStore;
import io.artur.eventsourcing.snapshots.SnapshotStore;
import io.artur.eventsourcing.unitofwork.EventSourcingUnitOfWork;

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
        
        // Create repository and services (DDD patterns)
        BankAccountRepository repository = new EventSourcingBankAccountRepository(eventStore);
        AccountValidationService validationService = new AccountValidationService(repository);
        OverdraftService overdraftService = new OverdraftService();
        
        // Create CQRS handlers
        OpenAccountCommandHandler commandHandler = new OpenAccountCommandHandler(eventStore, repository, validationService);
        GetAccountBalanceQueryHandler queryHandler = new GetAccountBalanceQueryHandler(repository);
        
        // Create Unit of Work for transaction management
        EventSourcingUnitOfWork unitOfWork = new EventSourcingUnitOfWork(repository);
        
        // Create a new account with overdraft protection and enhanced metadata
        BankAccount account = new BankAccount(eventStore);
        EventMetadata metadata = new EventMetadata("correlation-123", null, "user-456", 
                "Bank-App/1.0", "192.168.1.100", 1, Map.of("source", "main-demo"));
        account.openAccount("John Doe", BigDecimal.valueOf(200.00), metadata);
        UUID accountId = account.getAccountId();
        
        // Save using Unit of Work pattern
        unitOfWork.registerNew(account);
        unitOfWork.commit();
        
        LOGGER.info("Created new account with ID: " + accountId + " and overdraft limit: $" + account.getOverdraftLimit());
        
        // Demonstrate Value Objects
        Money depositAmount = Money.of(500.00);
        Money withdrawAmount1 = Money.of(150.00);
        Money withdrawAmount2 = Money.of(400.00);
        
        // Perform some operations
        account.deposit(depositAmount.getAmount());
        LOGGER.info("Deposited " + depositAmount + ", new balance: " + account.getBalanceAsMoney());
        
        account.withdraw(withdrawAmount1.getAmount());
        LOGGER.info("Withdrew " + withdrawAmount1 + ", new balance: " + account.getBalanceAsMoney());
        
        // Test overdraft functionality with domain service
        Money currentBalance = account.getBalanceAsMoney();
        Money overdraftLimit = account.getOverdraftLimitAsMoney();
        
        if (overdraftService.canWithdraw(currentBalance, overdraftLimit, withdrawAmount2)) {
            account.withdraw(withdrawAmount2.getAmount());
            LOGGER.info("Withdrew " + withdrawAmount2 + " (using overdraft), new balance: " + account.getBalanceAsMoney());
        }
        
        // Analyze overdraft situation
        OverdraftService.OverdraftAnalysis analysis = overdraftService.analyzeOverdraftSituation(
                account.getBalanceAsMoney(), account.getOverdraftLimitAsMoney());
        
        LOGGER.info("=== Overdraft Analysis ===");
        LOGGER.info("In overdraft: " + analysis.isInOverdraft());
        LOGGER.info("Overdraft amount: " + analysis.getOverdraftAmount());
        LOGGER.info("Available overdraft: " + analysis.getAvailableOverdraft());
        LOGGER.info("Maximum withdrawal: " + analysis.getMaximumWithdrawal());
        
        // Try to exceed overdraft limit using domain service validation
        Money largeWithdrawal = Money.of(200.00);
        try {
            if (!overdraftService.canWithdraw(account.getBalanceAsMoney(), account.getOverdraftLimitAsMoney(), largeWithdrawal)) {
                throw new OverdraftExceededException(account.getBalance(), account.getOverdraftLimit(), largeWithdrawal.getAmount());
            }
            account.withdraw(largeWithdrawal.getAmount());
        } catch (OverdraftExceededException e) {
            LOGGER.warning("Overdraft limit exceeded: " + e.getMessage());
        }
        
        // Demonstrate CQRS Query
        Money queryBalance = queryHandler.handle(new GetAccountBalanceQuery(accountId));
        LOGGER.info("Account balance via CQRS query: " + queryBalance);
        
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