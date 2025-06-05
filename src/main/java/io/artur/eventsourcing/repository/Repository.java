package io.artur.eventsourcing.repository;

import java.util.List;
import java.util.Optional;

public interface Repository<T, ID> {
    void save(T aggregate);
    Optional<T> findById(ID id);
    List<T> findAll();
    void delete(ID id);
    boolean exists(ID id);
}