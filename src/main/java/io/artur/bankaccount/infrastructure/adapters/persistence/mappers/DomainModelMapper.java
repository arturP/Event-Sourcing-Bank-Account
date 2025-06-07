package io.artur.bankaccount.infrastructure.adapters.persistence.mappers;

import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.account.events.*;
import io.artur.bankaccount.domain.account.valueobjects.AccountHolder;
import io.artur.bankaccount.domain.account.valueobjects.AccountNumber;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import io.artur.bankaccount.domain.shared.valueobjects.Money;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between new domain model and legacy infrastructure model
 */
public class DomainModelMapper {
    
    /**
     * Convert legacy BankAccount to new domain BankAccount
     */
    public static BankAccount fromLegacy(io.artur.eventsourcing.aggregates.BankAccount legacyAccount) {
        if (legacyAccount == null) {
            return null;
        }
        
        // Get events from legacy account
        List<io.artur.eventsourcing.events.AccountEvent> legacyEvents = legacyAccount.getEvents();
        
        // Convert legacy events to new domain events
        List<AccountDomainEvent> domainEvents = legacyEvents.stream()
                .map(DomainModelMapper::convertLegacyEvent)
                .collect(Collectors.toList());
        
        // Reconstruct domain account from events
        return BankAccount.fromHistory(legacyAccount.getAccountId(), domainEvents);
    }
    
    /**
     * Convert new domain BankAccount to legacy BankAccount
     * This is more complex as we need to replay events through the legacy model
     */
    public static io.artur.eventsourcing.aggregates.BankAccount toLegacy(
            BankAccount domainAccount, 
            io.artur.eventsourcing.eventstores.EventStore<io.artur.eventsourcing.events.AccountEvent, java.util.UUID> eventStore) {
        
        if (domainAccount == null) {
            return null;
        }
        
        // Try to load existing legacy account
        io.artur.eventsourcing.aggregates.BankAccount legacyAccount;
        try {
            legacyAccount = io.artur.eventsourcing.aggregates.BankAccount.loadFromStore(eventStore, domainAccount.getAccountId());
        } catch (IllegalArgumentException e) {
            // Account doesn't exist in legacy store, create new one
            legacyAccount = new io.artur.eventsourcing.aggregates.BankAccount(eventStore);
        }
        
        // Apply any uncommitted events from domain model to legacy model
        List<AccountDomainEvent> uncommittedEvents = domainAccount.getUncommittedEvents();
        for (AccountDomainEvent domainEvent : uncommittedEvents) {
            io.artur.eventsourcing.events.AccountEvent legacyEvent = convertDomainEvent(domainEvent);
            legacyAccount.apply(legacyEvent);
        }
        
        return legacyAccount;
    }
    
    /**
     * Convert legacy event to new domain event
     */
    private static AccountDomainEvent convertLegacyEvent(io.artur.eventsourcing.events.AccountEvent legacyEvent) {
        EventMetadata metadata = convertLegacyMetadata(legacyEvent.getMetadata());
        
        if (legacyEvent instanceof io.artur.eventsourcing.events.AccountOpenedEvent openEvent) {
            return new AccountOpenedEvent(
                openEvent.getId(),
                openEvent.getAccountHolder(),
                openEvent.getOverdraftLimit(),
                metadata
            );
        } else if (legacyEvent instanceof io.artur.eventsourcing.events.MoneyDepositedEvent depositEvent) {
            return new MoneyDepositedEvent(
                depositEvent.getId(),
                depositEvent.getAmount(),
                metadata
            );
        } else if (legacyEvent instanceof io.artur.eventsourcing.events.MoneyWithdrawnEvent withdrawEvent) {
            return new MoneyWithdrawnEvent(
                withdrawEvent.getId(),
                withdrawEvent.getAmount(),
                metadata
            );
        } else if (legacyEvent instanceof io.artur.eventsourcing.events.MoneyTransferredEvent transferEvent) {
            return new MoneyTransferredEvent(
                transferEvent.getId(),
                transferEvent.getToAccountId(),
                transferEvent.getAmount(),
                transferEvent.getDescription(),
                metadata
            );
        } else if (legacyEvent instanceof io.artur.eventsourcing.events.MoneyReceivedEvent receivedEvent) {
            return new MoneyReceivedEvent(
                receivedEvent.getId(),
                receivedEvent.getFromAccountId(),
                receivedEvent.getAmount(),
                receivedEvent.getDescription(),
                metadata
            );
        } else {
            throw new IllegalArgumentException("Unsupported legacy event type: " + legacyEvent.getClass().getName());
        }
    }
    
    /**
     * Convert new domain event to legacy event
     */
    private static io.artur.eventsourcing.events.AccountEvent convertDomainEvent(AccountDomainEvent domainEvent) {
        io.artur.eventsourcing.events.EventMetadata legacyMetadata = convertDomainMetadata(domainEvent.getMetadata());
        
        if (domainEvent instanceof AccountOpenedEvent openEvent) {
            return new io.artur.eventsourcing.events.AccountOpenedEvent(
                openEvent.getId(),
                openEvent.getAccountHolder(),
                openEvent.getOverdraftLimit(),
                legacyMetadata
            );
        } else if (domainEvent instanceof MoneyDepositedEvent depositEvent) {
            return new io.artur.eventsourcing.events.MoneyDepositedEvent(
                depositEvent.getId(),
                depositEvent.getAmount(),
                legacyMetadata
            );
        } else if (domainEvent instanceof MoneyWithdrawnEvent withdrawEvent) {
            return new io.artur.eventsourcing.events.MoneyWithdrawnEvent(
                withdrawEvent.getId(),
                withdrawEvent.getAmount(),
                legacyMetadata
            );
        } else if (domainEvent instanceof MoneyTransferredEvent transferEvent) {
            return new io.artur.eventsourcing.events.MoneyTransferredEvent(
                transferEvent.getId(),
                transferEvent.getToAccountId(),
                transferEvent.getAmount(),
                transferEvent.getDescription(),
                legacyMetadata
            );
        } else if (domainEvent instanceof MoneyReceivedEvent receivedEvent) {
            return new io.artur.eventsourcing.events.MoneyReceivedEvent(
                receivedEvent.getId(),
                receivedEvent.getFromAccountId(),
                receivedEvent.getAmount(),
                receivedEvent.getDescription(),
                legacyMetadata
            );
        } else {
            throw new IllegalArgumentException("Unsupported domain event type: " + domainEvent.getClass().getName());
        }
    }
    
    /**
     * Convert legacy metadata to new domain metadata
     */
    private static EventMetadata convertLegacyMetadata(io.artur.eventsourcing.events.EventMetadata legacyMetadata) {
        if (legacyMetadata == null) {
            return new EventMetadata(1);
        }
        
        return new EventMetadata(
            legacyMetadata.getCorrelationId(),
            legacyMetadata.getCausationId(),
            legacyMetadata.getUserId(),
            legacyMetadata.getUserAgent(),
            legacyMetadata.getIpAddress(),
            legacyMetadata.getVersion(),
            legacyMetadata.getAdditionalProperties()
        );
    }
    
    /**
     * Convert new domain metadata to legacy metadata
     */
    private static io.artur.eventsourcing.events.EventMetadata convertDomainMetadata(EventMetadata domainMetadata) {
        if (domainMetadata == null) {
            return new io.artur.eventsourcing.events.EventMetadata(1);
        }
        
        return new io.artur.eventsourcing.events.EventMetadata(
            domainMetadata.getCorrelationId(),
            domainMetadata.getCausationId(),
            domainMetadata.getUserId(),
            domainMetadata.getUserAgent(),
            domainMetadata.getIpAddress(),
            domainMetadata.getVersion(),
            domainMetadata.getAdditionalProperties()
        );
    }
    
    /**
     * Convert legacy AccountNumber to new domain AccountNumber
     */
    public static AccountNumber convertLegacyAccountNumber(io.artur.eventsourcing.domain.AccountNumber legacyAccountNumber) {
        if (legacyAccountNumber == null) {
            return null;
        }
        return new AccountNumber(legacyAccountNumber.getValue());
    }
    
    /**
     * Convert new domain AccountNumber to legacy AccountNumber
     */
    public static io.artur.eventsourcing.domain.AccountNumber convertDomainAccountNumber(AccountNumber domainAccountNumber) {
        if (domainAccountNumber == null) {
            return null;
        }
        return new io.artur.eventsourcing.domain.AccountNumber(domainAccountNumber.getValue());
    }
    
    /**
     * Convert legacy Money to new domain Money
     */
    public static Money convertLegacyMoney(io.artur.eventsourcing.domain.Money legacyMoney) {
        if (legacyMoney == null) {
            return Money.zero();
        }
        return new Money(legacyMoney.getAmount(), legacyMoney.getCurrency());
    }
    
    /**
     * Convert new domain Money to legacy Money
     */
    public static io.artur.eventsourcing.domain.Money convertDomainMoney(Money domainMoney) {
        if (domainMoney == null) {
            return io.artur.eventsourcing.domain.Money.zero();
        }
        return new io.artur.eventsourcing.domain.Money(domainMoney.getAmount(), domainMoney.getCurrency());
    }
}