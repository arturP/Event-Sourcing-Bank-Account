package io.artur.eventsourcing.eventstores;

import io.artur.eventsourcing.events.AccountEvent;

import java.util.List;

public interface EventStore<T extends AccountEvent, K> {

    void saveEvent(final K id, final T event);
    List<T> getEventStream(final K id);
    List<T> getEventStream(final K id, int offset, int limit);
    boolean isEmpty(final K id);
    long eventsCount(final K id);
    
    default EventPage<T> getEventPage(final K id, int offset, int limit) {
        List<T> events = getEventStream(id, offset, limit);
        long totalEvents = eventsCount(id);
        return new EventPage<>(events, offset, limit, totalEvents);
    }
    
    public static class EventPage<T> {
        private final List<T> events;
        private final int offset;
        private final int limit;
        private final long totalEvents;
        private final boolean hasMore;
        
        public EventPage(List<T> events, int offset, int limit, long totalEvents) {
            this.events = events;
            this.offset = offset;
            this.limit = limit;
            this.totalEvents = totalEvents;
            this.hasMore = (offset + events.size()) < totalEvents;
        }
        
        public List<T> getEvents() { return events; }
        public int getOffset() { return offset; }
        public int getLimit() { return limit; }
        public long getTotalEvents() { return totalEvents; }
        public boolean hasMore() { return hasMore; }
        public boolean isEmpty() { return events.isEmpty(); }
        public int size() { return events.size(); }
    }
}
