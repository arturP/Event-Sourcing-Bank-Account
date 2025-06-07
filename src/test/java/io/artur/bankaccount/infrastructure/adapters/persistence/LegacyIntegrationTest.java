package io.artur.bankaccount.infrastructure.adapters.persistence;

import io.artur.bankaccount.application.services.AccountApplicationService;
import io.artur.bankaccount.application.commands.models.*;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import io.artur.bankaccount.infrastructure.adapters.persistence.repositories.LegacyAccountRepositoryAdapter;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.InMemoryEventStore;
import io.artur.eventsourcing.repository.EventSourcingBankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that the new domain model works correctly with legacy infrastructure
 */
class LegacyIntegrationTest {
    
    private EventStore<AccountEvent, UUID> eventStore;
    private EventSourcingBankAccountRepository legacyRepository;
    private LegacyAccountRepositoryAdapter repositoryAdapter;
    private AccountApplicationService applicationService;
    private EventMetadata metadata;
    
    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore<>();
        legacyRepository = new EventSourcingBankAccountRepository(eventStore);
        repositoryAdapter = new LegacyAccountRepositoryAdapter(legacyRepository, eventStore);
        applicationService = new AccountApplicationService(repositoryAdapter);
        metadata = new EventMetadata(1);
    }
    
    @Test
    void shouldCreateAccountUsingNewDomainModelAndPersistToLegacyInfrastructure() {
        // Given
        String accountHolder = "John Doe";
        BigDecimal overdraftLimit = BigDecimal.valueOf(500);
        
        OpenAccountCommand command = new OpenAccountCommand(
            UUID.randomUUID(),
            accountHolder,
            overdraftLimit,
            metadata
        );
        
        // When
        UUID accountId = applicationService.openAccount(command);
        
        // Then
        assertNotNull(accountId);
        
        // Verify account exists in legacy repository
        assertTrue(legacyRepository.exists(accountId));
        
        // Verify we can retrieve it through the new domain model
        Optional<BankAccount> retrievedAccount = repositoryAdapter.findById(accountId);
        assertTrue(retrievedAccount.isPresent());
        assertEquals(accountHolder, retrievedAccount.get().getAccountHolder().getFullName());
        assertEquals(0, overdraftLimit.compareTo(retrievedAccount.get().getOverdraftLimit().getAmount()));
    }
    
    @Test
    void shouldPerformDepositUsingNewDomainModelAndPersistToLegacyInfrastructure() {
        // Given - create account first
        UUID accountId = UUID.randomUUID();
        OpenAccountCommand openCommand = new OpenAccountCommand(accountId, "Jane Smith", BigDecimal.valueOf(100), metadata);
        applicationService.openAccount(openCommand);
        
        // When - deposit money
        BigDecimal depositAmount = BigDecimal.valueOf(250);
        DepositMoneyCommand depositCommand = new DepositMoneyCommand(accountId, depositAmount, metadata);
        applicationService.deposit(depositCommand);
        
        // Then
        Optional<BankAccount> account = repositoryAdapter.findById(accountId);
        assertTrue(account.isPresent());
        assertEquals(0, depositAmount.compareTo(account.get().getBalance().getAmount()));
        
        // Verify events were persisted to legacy event store
        assertFalse(eventStore.isEmpty(accountId));
        assertTrue(eventStore.eventsCount(accountId) >= 2); // Account opened + deposit
    }
    
    @Test
    void shouldPerformWithdrawalUsingNewDomainModelAndPersistToLegacyInfrastructure() {
        // Given - create account and deposit money
        UUID accountId = UUID.randomUUID();
        OpenAccountCommand openCommand = new OpenAccountCommand(accountId, "Bob Wilson", BigDecimal.valueOf(200), metadata);
        applicationService.openAccount(openCommand);
        
        DepositMoneyCommand depositCommand = new DepositMoneyCommand(accountId, BigDecimal.valueOf(500), metadata);
        applicationService.deposit(depositCommand);
        
        // When - withdraw money
        BigDecimal withdrawAmount = BigDecimal.valueOf(150);
        WithdrawMoneyCommand withdrawCommand = new WithdrawMoneyCommand(accountId, withdrawAmount, metadata);
        applicationService.withdraw(withdrawCommand);
        
        // Then
        Optional<BankAccount> account = repositoryAdapter.findById(accountId);
        assertTrue(account.isPresent());
        assertEquals(0, BigDecimal.valueOf(350).compareTo(account.get().getBalance().getAmount()));
        
        // Verify events were persisted to legacy event store
        assertTrue(eventStore.eventsCount(accountId) >= 3); // Account opened + deposit + withdrawal
    }
    
    @Test
    void shouldPerformTransferUsingNewDomainModelAndPersistToLegacyInfrastructure() {
        // Given - create two accounts
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        
        OpenAccountCommand openFromCommand = new OpenAccountCommand(fromAccountId, "Alice Brown", BigDecimal.valueOf(100), metadata);
        OpenAccountCommand openToCommand = new OpenAccountCommand(toAccountId, "Charlie Davis", BigDecimal.valueOf(50), metadata);
        
        applicationService.openAccount(openFromCommand);
        applicationService.openAccount(openToCommand);
        
        // Deposit money in from account
        DepositMoneyCommand depositCommand = new DepositMoneyCommand(fromAccountId, BigDecimal.valueOf(1000), metadata);
        applicationService.deposit(depositCommand);
        
        // When - transfer money
        BigDecimal transferAmount = BigDecimal.valueOf(300);
        TransferMoneyCommand transferCommand = new TransferMoneyCommand(
            fromAccountId, 
            toAccountId, 
            transferAmount, 
            "Test transfer", 
            metadata
        );
        applicationService.transfer(transferCommand);
        
        // Then
        Optional<BankAccount> fromAccount = repositoryAdapter.findById(fromAccountId);
        Optional<BankAccount> toAccount = repositoryAdapter.findById(toAccountId);
        
        assertTrue(fromAccount.isPresent());
        assertTrue(toAccount.isPresent());
        
        assertEquals(0, BigDecimal.valueOf(700).compareTo(fromAccount.get().getBalance().getAmount()));
        assertEquals(0, transferAmount.compareTo(toAccount.get().getBalance().getAmount()));
        
        // Verify events were persisted for both accounts
        assertTrue(eventStore.eventsCount(fromAccountId) >= 3); // Open + deposit + transfer out
        assertTrue(eventStore.eventsCount(toAccountId) >= 2);   // Open + transfer in
    }
    
    @Test
    void shouldRetrieveAccountsCreatedInLegacyInfrastructure() {
        // Given - create account directly in legacy infrastructure
        io.artur.eventsourcing.aggregates.BankAccount legacyAccount = 
            new io.artur.eventsourcing.aggregates.BankAccount(eventStore);
        
        String accountHolder = "Legacy User";
        legacyAccount.openAccount(accountHolder, BigDecimal.valueOf(300));
        legacyAccount.deposit(BigDecimal.valueOf(100));
        legacyRepository.save(legacyAccount);
        
        UUID legacyAccountId = legacyAccount.getAccountId();
        
        // When - retrieve through new domain model
        Optional<BankAccount> retrievedAccount = repositoryAdapter.findById(legacyAccountId);
        
        // Then
        assertTrue(retrievedAccount.isPresent());
        assertEquals(accountHolder, retrievedAccount.get().getAccountHolder().getFullName());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(retrievedAccount.get().getBalance().getAmount()));
        assertEquals(0, BigDecimal.valueOf(300).compareTo(retrievedAccount.get().getOverdraftLimit().getAmount()));
    }
    
    @Test
    void shouldHandleOverdraftCorrectlyAcrossBothModels() {
        // Given
        UUID accountId = UUID.randomUUID();
        BigDecimal overdraftLimit = BigDecimal.valueOf(200);
        
        OpenAccountCommand openCommand = new OpenAccountCommand(accountId, "Test User", overdraftLimit, metadata);
        applicationService.openAccount(openCommand);
        
        DepositMoneyCommand depositCommand = new DepositMoneyCommand(accountId, BigDecimal.valueOf(100), metadata);
        applicationService.deposit(depositCommand);
        
        // When - try to withdraw more than balance + overdraft
        BigDecimal withdrawAmount = BigDecimal.valueOf(350); // More than 100 + 200
        WithdrawMoneyCommand withdrawCommand = new WithdrawMoneyCommand(accountId, withdrawAmount, metadata);
        
        // Then - should throw overdraft exception
        assertThrows(Exception.class, () -> {
            applicationService.withdraw(withdrawCommand);
        });
        
        // Balance should remain unchanged
        Optional<BankAccount> account = repositoryAdapter.findById(accountId);
        assertTrue(account.isPresent());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(account.get().getBalance().getAmount()));
    }
}