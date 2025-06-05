package io.artur.eventsourcing.eventstores;

import io.artur.eventsourcing.events.AccountEvent;

import java.util.*;

public class InMemoryEventStore<T extends AccountEvent, K> implements EventStore<T, K> {

    private final Map<K, List<T>> eventStream = new HashMap<>();

    public InMemoryEventStore(){}

    public InMemoryEventStore(final K id, final List<T> events) {
        eventStream.put(id, events);
    }

    @Override
    public void saveEvent(final K id, final T event) {
        eventStream.computeIfAbsent(id, v -> new ArrayList<>())
                .add(event);
    }

    @Override
    public List<T> getEventStream(final K id) {
        return eventStream.getOrDefault(id, Collections.emptyList());
    }

    @Override
    public List<T> getEventStream(final K id, int offset, int limit) {
        List<T> events = getEventStream(id);
        if (offset >= events.size()) {
            return Collections.emptyList();
        }
        int endIndex = Math.min(offset + limit, events.size());
        return events.subList(offset, endIndex);
    }

    @Override
    public boolean isEmpty(final K id) {
        return getEventStream(id).isEmpty();
    }

    @Override
    public long eventsCount(K id) {
        return getEventStream(id).size();
    }
}
