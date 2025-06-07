package io.artur.bankaccount.application.ports.outgoing;

import io.artur.bankaccount.domain.account.events.AccountDomainEvent;

import java.util.List;
import java.util.UUID;

public interface EventStore {
    
    void saveEvents(UUID aggregateId, List<AccountDomainEvent> events, int expectedVersion);
    
    List<AccountDomainEvent> getEventStream(UUID aggregateId);
    
    List<AccountDomainEvent> getEventStream(UUID aggregateId, int fromVersion);
    
    boolean isEmpty(UUID aggregateId);
    
    int getVersion(UUID aggregateId);
}