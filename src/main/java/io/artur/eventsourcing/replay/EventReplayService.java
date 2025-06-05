package io.artur.eventsourcing.replay;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class EventReplayService {
    
    private static final Logger LOGGER = Logger.getLogger(EventReplayService.class.getName());
    
    private final EventStore<AccountEvent, UUID> eventStore;
    
    public EventReplayService(EventStore<AccountEvent, UUID> eventStore) {
        this.eventStore = eventStore;
    }
    
    public void replayEvents(UUID accountId, Consumer<AccountEvent> eventHandler) {
        LOGGER.info("Starting event replay for account: " + accountId);
        
        List<AccountEvent> events = eventStore.getEventStream(accountId);
        LOGGER.info("Found " + events.size() + " events to replay");
        
        for (AccountEvent event : events) {
            LOGGER.fine("Replaying event: " + event.getClass().getSimpleName() + 
                       " at " + event.getTimestamp() + " (version " + event.getVersion() + ")");
            eventHandler.accept(event);
        }
        
        LOGGER.info("Event replay completed for account: " + accountId);
    }
    
    public void replayEventsWithFilter(UUID accountId, Predicate<AccountEvent> filter, Consumer<AccountEvent> eventHandler) {
        LOGGER.info("Starting filtered event replay for account: " + accountId);
        
        List<AccountEvent> events = eventStore.getEventStream(accountId);
        List<AccountEvent> filteredEvents = events.stream()
                .filter(filter)
                .toList();
                
        LOGGER.info("Found " + filteredEvents.size() + " events matching filter out of " + events.size() + " total events");
        
        for (AccountEvent event : filteredEvents) {
            LOGGER.fine("Replaying filtered event: " + event.getClass().getSimpleName() + 
                       " at " + event.getTimestamp() + " (version " + event.getVersion() + ")");
            eventHandler.accept(event);
        }
        
        LOGGER.info("Filtered event replay completed for account: " + accountId);
    }
    
    public BankAccount replayToPointInTime(UUID accountId, LocalDateTime pointInTime) {
        LOGGER.info("Replaying events to point in time: " + pointInTime + " for account: " + accountId);
        
        List<AccountEvent> events = eventStore.getEventStream(accountId);
        
        List<AccountEvent> eventsUpToPoint = events.stream()
                .filter(event -> event.getTimestamp().isBefore(pointInTime) || event.getTimestamp().isEqual(pointInTime))
                .toList();
                
        LOGGER.info("Replaying " + eventsUpToPoint.size() + " events up to " + pointInTime);
        
        // Use the reconstruct method instead of apply to avoid saving events
        BankAccount account = BankAccount.reconstruct(eventStore, eventsUpToPoint, null);
        
        LOGGER.info("Point-in-time replay completed. Final balance: " + account.getBalance());
        return account;
    }
    
    public void debugEventStream(UUID accountId) {
        LOGGER.info("=== DEBUG EVENT STREAM FOR ACCOUNT: " + accountId + " ===");
        
        List<AccountEvent> events = eventStore.getEventStream(accountId);
        
        for (int i = 0; i < events.size(); i++) {
            AccountEvent event = events.get(i);
            LOGGER.info(String.format("[%d] %s at %s (v%d) - Correlation: %s", 
                    i + 1,
                    event.getClass().getSimpleName(),
                    event.getTimestamp(),
                    event.getVersion(),
                    event.getMetadata() != null ? event.getMetadata().getCorrelationId() : "N/A"));
        }
        
        LOGGER.info("=== END DEBUG EVENT STREAM ===");
    }
}