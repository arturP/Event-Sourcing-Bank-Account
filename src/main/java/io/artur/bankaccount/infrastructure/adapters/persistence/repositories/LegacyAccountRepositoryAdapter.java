package io.artur.bankaccount.infrastructure.adapters.persistence.repositories;

import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.account.valueobjects.AccountNumber;
import io.artur.bankaccount.infrastructure.adapters.persistence.mappers.DomainModelMapper;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.repository.EventSourcingBankAccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter to bridge between new domain model and legacy infrastructure
 * This enables gradual migration from legacy to new architecture
 */
@Component
public class LegacyAccountRepositoryAdapter implements AccountRepository {
    
    private final EventSourcingBankAccountRepository legacyRepository;
    private final EventStore<AccountEvent, UUID> eventStore;
    
    public LegacyAccountRepositoryAdapter(EventSourcingBankAccountRepository legacyRepository,
                                        EventStore<AccountEvent, UUID> eventStore) {
        this.legacyRepository = legacyRepository;
        this.eventStore = eventStore;
    }
    
    @Override
    public void save(BankAccount account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        
        // Convert new domain model to legacy model and save
        io.artur.eventsourcing.aggregates.BankAccount legacyAccount = 
            DomainModelMapper.toLegacy(account, eventStore);
        
        legacyRepository.save(legacyAccount);
        
        // Mark domain events as committed since they're now persisted
        account.markEventsAsCommitted();
    }
    
    @Override
    public Optional<BankAccount> findById(UUID accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        
        // Load from legacy repository and convert to new domain model
        Optional<io.artur.eventsourcing.aggregates.BankAccount> legacyAccountOpt = 
            legacyRepository.findById(accountId);
        
        return legacyAccountOpt.map(DomainModelMapper::fromLegacy);
    }
    
    @Override
    public Optional<BankAccount> findByAccountNumber(AccountNumber accountNumber) {
        if (accountNumber == null) {
            return Optional.empty();
        }
        
        // Convert AccountNumber and delegate to legacy repository
        io.artur.eventsourcing.domain.AccountNumber legacyAccountNumber = 
            DomainModelMapper.convertDomainAccountNumber(accountNumber);
        
        Optional<io.artur.eventsourcing.aggregates.BankAccount> legacyAccountOpt = 
            legacyRepository.findByAccountNumber(legacyAccountNumber);
        
        return legacyAccountOpt.map(DomainModelMapper::fromLegacy);
    }
    
    @Override
    public List<BankAccount> findAll() {
        // Load all from legacy repository and convert to new domain model
        List<io.artur.eventsourcing.aggregates.BankAccount> legacyAccounts = 
            legacyRepository.findAll();
        
        return legacyAccounts.stream()
                .map(DomainModelMapper::fromLegacy)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<BankAccount> findByAccountHolder(String accountHolderName) {
        if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
            return List.of();
        }
        
        // Delegate to legacy repository and convert results
        List<io.artur.eventsourcing.aggregates.BankAccount> legacyAccounts = 
            legacyRepository.findByAccountHolder(accountHolderName);
        
        return legacyAccounts.stream()
                .map(DomainModelMapper::fromLegacy)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean exists(UUID accountId) {
        return legacyRepository.exists(accountId);
    }
    
    @Override
    public void delete(UUID accountId) {
        legacyRepository.delete(accountId);
    }
}