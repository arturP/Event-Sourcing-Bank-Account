package io.artur.eventsourcing.eventstores;

import io.artur.eventsourcing.events.AccountEvent;

import java.util.List;

public interface EventStore<T extends AccountEvent, K> {

    void saveEvent(final K id, final T event);
    List<T> getEventStream(final K id);
    boolean isEmpty(final K id);
    long eventsCount(final K id);
}
