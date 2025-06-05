package io.artur.eventsourcing.cqrs;

public interface QueryHandler<T, R> {
    R handle(T query);
}