package io.artur.bankaccount.infrastructure.persistence.eventstore.serialization;

import io.artur.bankaccount.domain.shared.events.DomainEvent;
import io.artur.bankaccount.domain.account.events.*;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Native event serializer that handles domain events without depending on legacy infrastructure
 */
@Component
public class EventSerializer {
    
    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends DomainEvent>> eventTypeRegistry;
    
    public EventSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.eventTypeRegistry = initializeEventTypeRegistry();
    }
    
    public String serialize(DomainEvent event) {
        try {
            EventWrapper wrapper = new EventWrapper();
            wrapper.eventType = event.getClass().getSimpleName();
            wrapper.aggregateId = event.getId();
            wrapper.metadata = event.getMetadata();
            
            // Serialize specific event data based on type
            if (event instanceof AccountOpenedEvent openedEvent) {
                wrapper.accountHolder = openedEvent.getAccountHolder();
                wrapper.overdraftLimit = openedEvent.getOverdraftLimit();
            } else if (event instanceof MoneyDepositedEvent depositedEvent) {
                wrapper.amount = depositedEvent.getAmount();
            } else if (event instanceof MoneyWithdrawnEvent withdrawnEvent) {
                wrapper.amount = withdrawnEvent.getAmount();
            } else if (event instanceof MoneyTransferredEvent transferredEvent) {
                wrapper.amount = transferredEvent.getAmount();
                wrapper.toAccountId = transferredEvent.getToAccountId();
                wrapper.description = transferredEvent.getDescription();
            } else if (event instanceof MoneyReceivedEvent receivedEvent) {
                wrapper.amount = receivedEvent.getAmount();
                wrapper.fromAccountId = receivedEvent.getFromAccountId();
                wrapper.description = receivedEvent.getDescription();
            }
            
            return objectMapper.writeValueAsString(wrapper);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event: " + event.getClass().getSimpleName(), e);
        }
    }
    
    public DomainEvent deserialize(String eventData, String eventType) {
        try {
            Class<? extends DomainEvent> eventClass = eventTypeRegistry.get(eventType);
            if (eventClass == null) {
                throw new IllegalArgumentException("Unknown event type: " + eventType);
            }
            
            JsonNode jsonNode = objectMapper.readTree(eventData);
            EventWrapper wrapper = objectMapper.treeToValue(jsonNode, EventWrapper.class);
            
            // Create specific event based on type
            return switch (eventType) {
                case "AccountOpenedEvent" -> new AccountOpenedEvent(
                    wrapper.aggregateId,
                    wrapper.accountHolder,
                    wrapper.overdraftLimit,
                    wrapper.metadata
                );
                case "MoneyDepositedEvent" -> new MoneyDepositedEvent(
                    wrapper.aggregateId,
                    wrapper.amount,
                    wrapper.metadata
                );
                case "MoneyWithdrawnEvent" -> new MoneyWithdrawnEvent(
                    wrapper.aggregateId,
                    wrapper.amount,
                    wrapper.metadata
                );
                case "MoneyTransferredEvent" -> new MoneyTransferredEvent(
                    wrapper.aggregateId,
                    wrapper.toAccountId,
                    wrapper.amount,
                    wrapper.description != null ? wrapper.description : "",
                    wrapper.metadata
                );
                case "MoneyReceivedEvent" -> new MoneyReceivedEvent(
                    wrapper.aggregateId,
                    wrapper.fromAccountId,
                    wrapper.amount,
                    wrapper.description != null ? wrapper.description : "",
                    wrapper.metadata
                );
                default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
            };
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event: " + eventType, e);
        }
    }
    
    private Map<String, Class<? extends DomainEvent>> initializeEventTypeRegistry() {
        Map<String, Class<? extends DomainEvent>> registry = new HashMap<>();
        registry.put("AccountOpenedEvent", AccountOpenedEvent.class);
        registry.put("MoneyDepositedEvent", MoneyDepositedEvent.class);
        registry.put("MoneyWithdrawnEvent", MoneyWithdrawnEvent.class);
        registry.put("MoneyTransferredEvent", MoneyTransferredEvent.class);
        registry.put("MoneyReceivedEvent", MoneyReceivedEvent.class);
        return registry;
    }
    
    /**
     * Wrapper class for JSON serialization/deserialization
     */
    private static class EventWrapper {
        public String eventType;
        public UUID aggregateId;
        public EventMetadata metadata;
        
        // Event-specific fields
        public String accountHolder;
        public BigDecimal overdraftLimit;
        public BigDecimal amount;
        public UUID toAccountId;
        public UUID fromAccountId;
        public String description;
    }
}