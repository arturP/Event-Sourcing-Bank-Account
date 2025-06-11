package io.artur.bankaccount.infrastructure.persistence.repositories;

import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.application.ports.outgoing.EventStorePort;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.account.events.AccountDomainEvent;
import io.artur.bankaccount.domain.account.valueobjects.AccountNumber;
import io.artur.bankaccount.domain.shared.events.DomainEvent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

/**
 * Native account repository implementation that uses the native event store
 * without depending on legacy infrastructure
 */
@Repository
public class NativeAccountRepository implements AccountRepository {
    
    private final EventStorePort eventStore;
    
    public NativeAccountRepository(EventStorePort eventStore) {
        this.eventStore = eventStore;
    }
    
    @Override
    public void save(BankAccount account) {
        List<AccountDomainEvent> uncommittedEvents = account.getUncommittedEvents();
        
        for (AccountDomainEvent event : uncommittedEvents) {
            eventStore.saveEvent(account.getAccountId(), event);
        }
        
        account.markEventsAsCommitted();
    }
    
    @Override
    public Optional<BankAccount> findById(UUID accountId) {
        if (!eventStore.hasEvents(accountId)) {
            return Optional.empty();
        }
        
        List<DomainEvent> events = eventStore.loadEvents(accountId);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        
        // Convert DomainEvent to AccountDomainEvent
        List<AccountDomainEvent> accountEvents = events.stream()
            .filter(event -> event instanceof AccountDomainEvent)
            .map(event -> (AccountDomainEvent) event)
            .collect(Collectors.toList());
        
        BankAccount account = BankAccount.fromHistory(accountId, accountEvents);
        return Optional.of(account);
    }
    
    @Override
    public Optional<BankAccount> findByAccountNumber(AccountNumber accountNumber) {
        // Note: This would require implementing account indexing by account number
        throw new UnsupportedOperationException(
            "findByAccountNumber() is not supported in this event-sourced implementation. " +
            "Consider implementing account number indexing or using projections for this query."
        );
    }
    
    @Override
    public List<BankAccount> findAll() {
        // Note: This is a simplified implementation for demonstration
        // In a real system, you would need to track all account IDs separately
        // or implement a more sophisticated query mechanism
        throw new UnsupportedOperationException(
            "findAll() is not supported in this event-sourced implementation. " +
            "Consider implementing account indexing or using projections for this query."
        );
    }
    
    @Override
    public List<BankAccount> findByAccountHolder(String accountHolderName) {
        // Note: This would require implementing account indexing by account holder
        throw new UnsupportedOperationException(
            "findByAccountHolder() is not supported in this event-sourced implementation. " +
            "Consider implementing account holder indexing or using projections for this query."
        );
    }
    
    @Override
    public boolean exists(UUID accountId) {
        return eventStore.hasEvents(accountId);
    }
    
    @Override
    public void delete(UUID accountId) {
        // In event sourcing, we typically don't delete events
        // Instead, we would add a "AccountClosedEvent" or similar
        throw new UnsupportedOperationException(
            "delete() is not supported in event sourcing. " +
            "Consider adding an AccountClosedEvent instead."
        );
    }
    
    /**
     * Load account from a specific version for point-in-time reconstruction
     */
    public Optional<BankAccount> findByIdAtVersion(UUID accountId, long version) {
        if (!eventStore.hasEvents(accountId)) {
            return Optional.empty();
        }
        
        EventStorePort.EventPage page = eventStore.loadEvents(accountId, 0, (int) version);
        List<DomainEvent> events = page.getEvents();
        
        if (events.isEmpty()) {
            return Optional.empty();
        }
        
        // Convert DomainEvent to AccountDomainEvent
        List<AccountDomainEvent> accountEvents = events.stream()
            .filter(event -> event instanceof AccountDomainEvent)
            .map(event -> (AccountDomainEvent) event)
            .collect(Collectors.toList());
        
        BankAccount account = BankAccount.fromHistory(accountId, accountEvents);
        return Optional.of(account);
    }
    
    /**
     * Load events for an account starting from a specific version
     */
    public Optional<BankAccount> findByIdFromVersion(UUID accountId, long fromVersion) {
        if (!eventStore.hasEvents(accountId)) {
            return Optional.empty();
        }
        
        List<DomainEvent> events = eventStore.loadEventsFromVersion(accountId, fromVersion);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        
        // Convert DomainEvent to AccountDomainEvent
        List<AccountDomainEvent> accountEvents = events.stream()
            .filter(event -> event instanceof AccountDomainEvent)
            .map(event -> (AccountDomainEvent) event)
            .collect(Collectors.toList());
        
        BankAccount account = BankAccount.fromHistory(accountId, accountEvents);
        return Optional.of(account);
    }
    
    /**
     * Get the current version of an account
     */
    public long getCurrentVersion(UUID accountId) {
        return eventStore.getLatestVersion(accountId);
    }
    
    /**
     * Check if account has any events
     */
    public boolean hasEvents(UUID accountId) {
        return eventStore.hasEvents(accountId);
    }
    
    /**
     * Get event count for an account
     */
    public long getEventCount(UUID accountId) {
        return eventStore.getEventCount(accountId);
    }
    
    @Override
    public CompletableFuture<Void> saveAsync(BankAccount account) {
        return CompletableFuture.runAsync(() -> save(account));
    }
    
    @Override
    public CompletableFuture<Optional<BankAccount>> findByIdAsync(UUID accountId) {
        return CompletableFuture.supplyAsync(() -> findById(accountId));
    }
    
    @Override
    public CompletableFuture<List<BankAccount>> findAllAsync() {
        return CompletableFuture.supplyAsync(this::findAll);
    }
}