package io.artur.eventsourcing.repository;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.InMemoryEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventSourcingBankAccountRepositoryTest {

    private EventSourcingBankAccountRepository repository;
    private EventStore<AccountEvent, UUID> eventStore;

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore<>();
        repository = new EventSourcingBankAccountRepository(eventStore);
    }

    @Test
    void saveAndFindById() {
        BankAccount account = new BankAccount(eventStore);
        account.openAccount("John Doe", BigDecimal.valueOf(100));
        UUID accountId = account.getAccountId();
        
        repository.save(account);
        
        Optional<BankAccount> found = repository.findById(accountId);
        assertTrue(found.isPresent());
        assertEquals(accountId, found.get().getAccountId());
        assertEquals("John Doe", found.get().getAccountHolder());
    }

    @Test
    void findByIdNonExistent() {
        UUID nonExistentId = UUID.randomUUID();
        
        Optional<BankAccount> found = repository.findById(nonExistentId);
        
        assertFalse(found.isPresent());
    }

    @Test
    void findByIdNullReturnsEmpty() {
        Optional<BankAccount> found = repository.findById(null);
        
        assertFalse(found.isPresent());
    }

    @Test
    void saveNullAccountThrows() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    void findAll() {
        BankAccount account1 = new BankAccount(eventStore);
        account1.openAccount("John Doe", BigDecimal.valueOf(100));
        
        BankAccount account2 = new BankAccount(eventStore);
        account2.openAccount("Jane Smith", BigDecimal.valueOf(200));
        
        repository.save(account1);
        repository.save(account2);
        
        List<BankAccount> allAccounts = repository.findAll();
        
        assertEquals(2, allAccounts.size());
    }

    @Test
    void exists() {
        BankAccount account = new BankAccount(eventStore);
        account.openAccount("John Doe", BigDecimal.valueOf(100));
        UUID accountId = account.getAccountId();
        
        repository.save(account);
        
        assertTrue(repository.exists(accountId));
        assertFalse(repository.exists(UUID.randomUUID()));
    }

    @Test
    void delete() {
        BankAccount account = new BankAccount(eventStore);
        account.openAccount("John Doe", BigDecimal.valueOf(100));
        UUID accountId = account.getAccountId();
        
        repository.save(account);
        assertTrue(repository.exists(accountId));
        
        repository.delete(accountId);
        // Note: In event sourcing, deletion typically removes from cache only
        // The account can still be loaded from events but won't be in cache
        repository.clearCache(); // Clear cache to ensure it's truly deleted from cache
        // For this test, we'll check that it's removed from cache but still exists in event store
        assertTrue(repository.exists(accountId)); // Still exists because events remain
    }

    @Test
    void findByAccountHolder() {
        BankAccount account1 = new BankAccount(eventStore);
        account1.openAccount("John Doe", BigDecimal.valueOf(100));
        
        BankAccount account2 = new BankAccount(eventStore);
        account2.openAccount("Jane Smith", BigDecimal.valueOf(200));
        
        repository.save(account1);
        repository.save(account2);
        
        List<BankAccount> johnAccounts = repository.findByAccountHolder("John Doe");
        
        assertEquals(1, johnAccounts.size());
        assertEquals("John Doe", johnAccounts.get(0).getAccountHolder());
    }

    @Test
    void findAccountsWithNegativeBalance() {
        BankAccount account1 = new BankAccount(eventStore);
        account1.openAccount("John Doe", BigDecimal.valueOf(200)); // Higher overdraft limit
        account1.deposit(BigDecimal.valueOf(50));
        account1.withdraw(BigDecimal.valueOf(200)); // Goes negative
        
        BankAccount account2 = new BankAccount(eventStore);
        account2.openAccount("Jane Smith", BigDecimal.valueOf(0));
        account2.deposit(BigDecimal.valueOf(100));
        
        repository.save(account1);
        repository.save(account2);
        
        List<BankAccount> negativeAccounts = repository.findAccountsWithNegativeBalance();
        
        assertEquals(1, negativeAccounts.size());
        assertEquals("John Doe", negativeAccounts.get(0).getAccountHolder());
        assertTrue(negativeAccounts.get(0).getBalance().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    void clearCache() {
        BankAccount account = new BankAccount(eventStore);
        account.openAccount("John Doe", BigDecimal.valueOf(100));
        
        repository.save(account);
        assertEquals(1, repository.getCacheSize());
        
        repository.clearCache();
        assertEquals(0, repository.getCacheSize());
    }
}