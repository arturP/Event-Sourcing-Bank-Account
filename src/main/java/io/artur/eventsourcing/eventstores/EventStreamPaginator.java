package io.artur.eventsourcing.eventstores;

import io.artur.eventsourcing.events.AccountEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class EventStreamPaginator<T extends AccountEvent, K> implements Iterable<EventStore.EventPage<T>> {
    
    private final EventStore<T, K> eventStore;
    private final K aggregateId;
    private final int pageSize;
    
    public EventStreamPaginator(EventStore<T, K> eventStore, K aggregateId, int pageSize) {
        this.eventStore = eventStore;
        this.aggregateId = aggregateId;
        this.pageSize = pageSize > 0 ? pageSize : 10;
    }
    
    public static <T extends AccountEvent, K> EventStreamPaginator<T, K> of(
            EventStore<T, K> eventStore, K aggregateId, int pageSize) {
        return new EventStreamPaginator<>(eventStore, aggregateId, pageSize);
    }
    
    public static <T extends AccountEvent, K> EventStreamPaginator<T, K> of(
            EventStore<T, K> eventStore, K aggregateId) {
        return new EventStreamPaginator<>(eventStore, aggregateId, 10);
    }
    
    public EventStore.EventPage<T> getPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        
        int offset = pageNumber * pageSize;
        long totalEvents = eventStore.eventsCount(aggregateId);
        List<T> events = eventStore.getEventStream(aggregateId, offset, pageSize);
        
        return new EventStore.EventPage<>(events, offset, pageSize, totalEvents);
    }
    
    public List<T> getEventsFromPage(int pageNumber) {
        return getPage(pageNumber).getEvents();
    }
    
    public void processAllPages(Consumer<EventStore.EventPage<T>> pageProcessor) {
        for (EventStore.EventPage<T> page : this) {
            pageProcessor.accept(page);
            if (!page.hasMore()) {
                break;
            }
        }
    }
    
    public void processAllEvents(Consumer<T> eventProcessor) {
        processAllPages(page -> page.getEvents().forEach(eventProcessor));
    }
    
    public List<T> collectAllEvents() {
        List<T> allEvents = new ArrayList<>();
        processAllEvents(allEvents::add);
        return allEvents;
    }
    
    public long getTotalEvents() {
        return eventStore.eventsCount(aggregateId);
    }
    
    public int getTotalPages() {
        long totalEvents = getTotalEvents();
        return (int) Math.ceil((double) totalEvents / pageSize);
    }
    
    public boolean hasEvents() {
        return getTotalEvents() > 0;
    }
    
    @Override
    public Iterator<EventStore.EventPage<T>> iterator() {
        return new PaginatedEventIterator();
    }
    
    private class PaginatedEventIterator implements Iterator<EventStore.EventPage<T>> {
        private int currentPage = 0;
        private EventStore.EventPage<T> currentEventPage;
        private boolean hasNextCached = false;
        private boolean hasNextValue = false;
        
        @Override
        public boolean hasNext() {
            if (!hasNextCached) {
                currentEventPage = getPage(currentPage);
                hasNextValue = !currentEventPage.isEmpty();
                hasNextCached = true;
            }
            return hasNextValue;
        }
        
        @Override
        public EventStore.EventPage<T> next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more pages available");
            }
            
            EventStore.EventPage<T> result = currentEventPage;
            currentPage++;
            hasNextCached = false;
            
            return result;
        }
    }
}