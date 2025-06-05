package io.artur.eventsourcing.unitofwork;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.repository.BankAccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class EventSourcingUnitOfWork implements UnitOfWork {
    
    private static final Logger LOGGER = Logger.getLogger(EventSourcingUnitOfWork.class.getName());
    
    private final BankAccountRepository repository;
    private final ConcurrentMap<Object, EntityState> trackedEntities;
    private boolean isCompleted;
    
    public EventSourcingUnitOfWork(BankAccountRepository repository) {
        this.repository = repository;
        this.trackedEntities = new ConcurrentHashMap<>();
        this.isCompleted = false;
    }
    
    @Override
    public void registerNew(Object entity) {
        checkNotCompleted();
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        trackedEntities.put(entity, EntityState.NEW);
        LOGGER.fine("Registered new entity: " + entity.getClass().getSimpleName());
    }
    
    @Override
    public void registerDirty(Object entity) {
        checkNotCompleted();
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        EntityState currentState = trackedEntities.get(entity);
        if (currentState != EntityState.NEW) {
            trackedEntities.put(entity, EntityState.DIRTY);
            LOGGER.fine("Registered dirty entity: " + entity.getClass().getSimpleName());
        }
    }
    
    @Override
    public void registerRemoved(Object entity) {
        checkNotCompleted();
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        EntityState currentState = trackedEntities.get(entity);
        if (currentState == EntityState.NEW) {
            trackedEntities.remove(entity);
        } else {
            trackedEntities.put(entity, EntityState.REMOVED);
        }
        LOGGER.fine("Registered removed entity: " + entity.getClass().getSimpleName());
    }
    
    @Override
    public void commit() {
        checkNotCompleted();
        
        try {
            LOGGER.info("Committing unit of work with " + trackedEntities.size() + " tracked entities");
            
            List<Exception> errors = new ArrayList<>();
            
            // Process all tracked entities
            for (var entry : trackedEntities.entrySet()) {
                Object entity = entry.getKey();
                EntityState state = entry.getValue();
                
                try {
                    switch (state) {
                        case NEW, DIRTY -> {
                            if (entity instanceof BankAccount account) {
                                repository.save(account);
                                LOGGER.fine("Saved account: " + account.getAccountId());
                            }
                        }
                        case REMOVED -> {
                            if (entity instanceof BankAccount account) {
                                repository.delete(account.getAccountId());
                                LOGGER.fine("Deleted account: " + account.getAccountId());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.severe("Error processing entity " + entity.getClass().getSimpleName() + ": " + e.getMessage());
                    errors.add(e);
                }
            }
            
            if (!errors.isEmpty()) {
                throw new UnitOfWorkException("Failed to commit some entities", errors);
            }
            
            LOGGER.info("Unit of work committed successfully");
            
        } finally {
            markCompleted();
        }
    }
    
    @Override
    public void rollback() {
        checkNotCompleted();
        
        try {
            LOGGER.info("Rolling back unit of work with " + trackedEntities.size() + " tracked entities");
            
            // In event sourcing, rollback typically means not persisting the events
            // Since we haven't committed yet, we just clear the tracked entities
            trackedEntities.clear();
            
            LOGGER.info("Unit of work rolled back successfully");
            
        } finally {
            markCompleted();
        }
    }
    
    @Override
    public <T> T executeInTransaction(Supplier<T> operation) {
        checkNotCompleted();
        
        try {
            T result = operation.get();
            commit();
            return result;
        } catch (Exception e) {
            rollback();
            throw new UnitOfWorkException("Transaction failed", e);
        }
    }
    
    @Override
    public void executeInTransaction(Runnable operation) {
        executeInTransaction(() -> {
            operation.run();
            return null;
        });
    }
    
    private void checkNotCompleted() {
        if (isCompleted) {
            throw new IllegalStateException("Unit of work has already been completed");
        }
    }
    
    private void markCompleted() {
        isCompleted = true;
        trackedEntities.clear();
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public int getTrackedEntityCount() {
        return trackedEntities.size();
    }
    
    private enum EntityState {
        NEW, DIRTY, REMOVED
    }
    
    public static class UnitOfWorkException extends RuntimeException {
        private final List<Exception> causes;
        
        public UnitOfWorkException(String message, Exception cause) {
            super(message, cause);
            this.causes = List.of(cause);
        }
        
        public UnitOfWorkException(String message, List<Exception> causes) {
            super(message);
            this.causes = new ArrayList<>(causes);
        }
        
        public List<Exception> getCauses() {
            return new ArrayList<>(causes);
        }
    }
}