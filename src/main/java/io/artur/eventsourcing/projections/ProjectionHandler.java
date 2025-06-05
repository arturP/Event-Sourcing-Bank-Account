package io.artur.eventsourcing.projections;

import io.artur.eventsourcing.events.AccountEvent;

public interface ProjectionHandler<T> {
    void handle(AccountEvent event);
    T getProjection();
    void reset();
}