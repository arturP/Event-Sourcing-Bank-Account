package io.artur.eventsourcing.unitofwork;

import java.util.function.Supplier;

public interface UnitOfWork {
    void registerNew(Object entity);
    void registerDirty(Object entity);
    void registerRemoved(Object entity);
    void commit();
    void rollback();
    <T> T executeInTransaction(Supplier<T> operation);
    void executeInTransaction(Runnable operation);
}