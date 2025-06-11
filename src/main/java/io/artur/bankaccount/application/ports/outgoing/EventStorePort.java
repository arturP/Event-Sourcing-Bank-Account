package io.artur.bankaccount.application.ports.outgoing;

import io.artur.bankaccount.domain.shared.events.DomainEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Port for event store operations in the new domain model
 * This abstracts away the specific event store implementation
 */
public interface EventStorePort {
    
    /**
     * Save a domain event for the specified aggregate
     */
    void saveEvent(UUID aggregateId, DomainEvent event);
    
    /**
     * Save a domain event for the specified aggregate asynchronously
     */
    CompletableFuture<Void> saveEventAsync(UUID aggregateId, DomainEvent event);
    
    /**
     * Load all events for the specified aggregate
     */
    List<DomainEvent> loadEvents(UUID aggregateId);
    
    /**
     * Load all events for the specified aggregate asynchronously
     */
    CompletableFuture<List<DomainEvent>> loadEventsAsync(UUID aggregateId);
    
    /**
     * Load events for the specified aggregate starting from a specific version
     */
    List<DomainEvent> loadEventsFromVersion(UUID aggregateId, long fromVersion);
    
    /**
     * Load events for the specified aggregate starting from a specific version asynchronously
     */
    CompletableFuture<List<DomainEvent>> loadEventsFromVersionAsync(UUID aggregateId, long fromVersion);
    
    /**
     * Load events for the specified aggregate with pagination
     */
    EventPage loadEvents(UUID aggregateId, int offset, int limit);
    
    /**
     * Load events for the specified aggregate with pagination asynchronously
     */
    CompletableFuture<EventPage> loadEventsAsync(UUID aggregateId, int offset, int limit);
    
    /**
     * Check if an aggregate has any events
     */
    boolean hasEvents(UUID aggregateId);
    
    /**
     * Get the total number of events for an aggregate
     */
    long getEventCount(UUID aggregateId);
    
    /**
     * Get the latest version (event count) for an aggregate
     */
    long getLatestVersion(UUID aggregateId);
    
    /**
     * Represents a page of events with pagination metadata
     */
    class EventPage {
        private final List<DomainEvent> events;
        private final int offset;
        private final int limit;
        private final long totalEvents;
        private final boolean hasMore;
        
        public EventPage(List<DomainEvent> events, int offset, int limit, long totalEvents) {
            this.events = events;
            this.offset = offset;
            this.limit = limit;
            this.totalEvents = totalEvents;
            this.hasMore = (offset + events.size()) < totalEvents;
        }
        
        public List<DomainEvent> getEvents() { return events; }
        public int getOffset() { return offset; }
        public int getLimit() { return limit; }
        public long getTotalEvents() { return totalEvents; }
        public boolean hasMore() { return hasMore; }
        public boolean isEmpty() { return events.isEmpty(); }
        public int size() { return events.size(); }
    }
}