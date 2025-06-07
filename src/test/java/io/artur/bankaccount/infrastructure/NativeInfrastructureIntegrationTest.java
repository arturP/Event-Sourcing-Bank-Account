package io.artur.bankaccount.infrastructure;

import io.artur.bankaccount.application.commands.models.DepositMoneyCommand;
import io.artur.bankaccount.application.commands.models.OpenAccountCommand;
import io.artur.bankaccount.application.commands.models.WithdrawMoneyCommand;
import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.application.ports.outgoing.CachePort;
import io.artur.bankaccount.application.ports.outgoing.EventStorePort;
import io.artur.bankaccount.application.ports.outgoing.MetricsPort;
import io.artur.bankaccount.application.services.AccountApplicationService;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import io.artur.bankaccount.domain.shared.valueobjects.Money;
import io.artur.bankaccount.infrastructure.monitoring.NativeMetricsCollector;
import io.artur.bankaccount.infrastructure.persistence.cache.NativeCacheService;
import io.artur.bankaccount.infrastructure.persistence.eventstore.NativeEventStore;
import io.artur.bankaccount.infrastructure.persistence.eventstore.serialization.EventSerializer;
import io.artur.bankaccount.infrastructure.persistence.repositories.NativeAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify native infrastructure works correctly
 */
class NativeInfrastructureIntegrationTest {
    
    private AccountApplicationService accountApplicationService;
    private AccountRepository accountRepository;
    private EventStorePort eventStorePort;
    private CachePort cachePort;
    private MetricsPort metricsPort;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create in-memory H2 database
        DataSource dataSource = createTestDataSource();
        
        // Create native infrastructure components
        EventSerializer eventSerializer = new EventSerializer();
        eventStorePort = new NativeEventStore(dataSource, eventSerializer);
        cachePort = new NativeCacheService();
        metricsPort = new NativeMetricsCollector();
        accountRepository = new NativeAccountRepository(eventStorePort);
        
        // Create application service
        accountApplicationService = new AccountApplicationService(accountRepository, cachePort, metricsPort);
    }
    
    private DataSource createTestDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setDriverClassName("org.h2.Driver");
        return dataSource;
    }
    
    
    @Test
    void shouldCreateAccountWithNativeInfrastructure() {
        // Given
        String accountHolderName = "Native Test User";
        BigDecimal overdraftLimit = BigDecimal.valueOf(1000);
        OpenAccountCommand command = new OpenAccountCommand(UUID.randomUUID(), accountHolderName, overdraftLimit, new EventMetadata(1));
        
        // When
        UUID accountId = accountApplicationService.openAccount(command);
        
        // Then
        assertNotNull(accountId);
        assertTrue(accountRepository.exists(accountId));
        assertTrue(eventStorePort.hasEvents(accountId));
        assertEquals(1, eventStorePort.getEventCount(accountId));
    }
    
    @Test
    void shouldPerformDepositWithNativeInfrastructure() {
        // Given
        String accountHolderName = "Native Deposit Test";
        BigDecimal overdraftLimit = BigDecimal.valueOf(500);
        UUID accountId = UUID.randomUUID();
        OpenAccountCommand openCommand = new OpenAccountCommand(accountId, accountHolderName, overdraftLimit, new EventMetadata(1));
        accountApplicationService.openAccount(openCommand);
        
        BigDecimal depositAmount = BigDecimal.valueOf(250);
        DepositMoneyCommand depositCommand = new DepositMoneyCommand(accountId, depositAmount, new EventMetadata(2));
        
        // When
        accountApplicationService.deposit(depositCommand);
        
        // Then
        Optional<BankAccount> accountOpt = accountRepository.findById(accountId);
        assertTrue(accountOpt.isPresent());
        
        BankAccount account = accountOpt.get();
        assertEquals(0, depositAmount.compareTo(account.getBalance().getAmount()));
        assertEquals(2, eventStorePort.getEventCount(accountId));
    }
    
    @Test
    void shouldUseNativeCaching() {
        // Given
        UUID accountId = UUID.randomUUID();
        Money balance = new Money(BigDecimal.valueOf(1500));
        
        // When
        cachePort.updateBalance(accountId, balance);
        
        // Then
        Optional<Money> cachedBalance = cachePort.getCachedBalance(accountId);
        assertTrue(cachedBalance.isPresent());
        assertEquals(0, balance.getAmount().compareTo(cachedBalance.get().getAmount()));
        
        // Test cache statistics
        CachePort.CacheStatistics stats = cachePort.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.getTotalRequests() > 0);
    }
    
    @Test
    void shouldUseNativeMetrics() {
        // When
        metricsPort.recordAccountCreation();
        metricsPort.recordDeposit();
        metricsPort.recordCacheHit("test");
        metricsPort.recordCacheMiss("test");
        
        // Then
        MetricsPort.PerformanceSummary summary = metricsPort.getPerformanceSummary();
        assertNotNull(summary);
        assertNotNull(summary.getBusinessMetrics());
        assertNotNull(summary.getCacheMetrics());
        
        MetricsPort.BusinessMetrics businessMetrics = summary.getBusinessMetrics();
        assertTrue(businessMetrics.getAccountsCreated() >= 1);
        assertTrue(businessMetrics.getDepositsProcessed() >= 1);
        
        MetricsPort.CacheMetrics cacheMetrics = summary.getCacheMetrics();
        assertTrue(cacheMetrics.getTotalHits() >= 1);
        assertTrue(cacheMetrics.getTotalMisses() >= 1);
    }
    
    @Test
    void shouldPersistEventsCorrectly() {
        // Given
        String accountHolderName = "Native Persistence Test";
        BigDecimal overdraftLimit = BigDecimal.valueOf(300);
        UUID accountId = UUID.randomUUID();
        
        // When - Create account and perform multiple operations
        OpenAccountCommand openCommand = new OpenAccountCommand(accountId, accountHolderName, overdraftLimit, new EventMetadata(1));
        accountApplicationService.openAccount(openCommand);
        
        DepositMoneyCommand depositCommand = new DepositMoneyCommand(accountId, BigDecimal.valueOf(500), new EventMetadata(2));
        accountApplicationService.deposit(depositCommand);
        
        WithdrawMoneyCommand withdrawCommand = new WithdrawMoneyCommand(accountId, BigDecimal.valueOf(100), new EventMetadata(3));
        accountApplicationService.withdraw(withdrawCommand);
        
        // Then - Verify event persistence
        assertEquals(3, eventStorePort.getEventCount(accountId));
        assertEquals(3, eventStorePort.getLatestVersion(accountId));
        
        // Verify account can be reconstructed from events
        Optional<BankAccount> reconstructedAccount = accountRepository.findById(accountId);
        assertTrue(reconstructedAccount.isPresent());
        
        BankAccount account = reconstructedAccount.get();
        assertEquals(0, BigDecimal.valueOf(400).compareTo(account.getBalance().getAmount())); // 500 - 100
        assertEquals(accountHolderName, account.getAccountHolder().getFullName());
        assertEquals(0, overdraftLimit.compareTo(account.getOverdraftLimit().getAmount()));
    }
}