package io.artur.bankaccount.infrastructure.persistence.eventstore;

import io.artur.bankaccount.application.ports.outgoing.EventStorePort;
import io.artur.bankaccount.domain.shared.events.DomainEvent;
import io.artur.bankaccount.infrastructure.persistence.eventstore.serialization.EventSerializer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Native event store implementation that directly implements EventStorePort
 * without depending on legacy infrastructure
 */
@Component
public class NativeEventStore implements EventStorePort {
    
    private final DataSource dataSource;
    private final EventSerializer eventSerializer;
    private final ConcurrentHashMap<UUID, AtomicLong> versionCounters = new ConcurrentHashMap<>();
    private final ExecutorService eventProcessingExecutor;
    private final ExecutorService dbOperationExecutor;
    
    // SQL statements
    private static final String INSERT_EVENT_SQL = 
        "INSERT INTO events (aggregate_id, event_type, event_data, event_version, created_at, correlation_id) " +
        "VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT_EVENTS_SQL = 
        "SELECT event_type, event_data, event_version, created_at, correlation_id " +
        "FROM events WHERE aggregate_id = ? ORDER BY event_version ASC";
    
    private static final String SELECT_EVENTS_PAGINATED_SQL = 
        "SELECT event_type, event_data, event_version, created_at, correlation_id " +
        "FROM events WHERE aggregate_id = ? ORDER BY event_version ASC LIMIT ? OFFSET ?";
    
    private static final String SELECT_EVENTS_FROM_VERSION_SQL = 
        "SELECT event_type, event_data, event_version, created_at, correlation_id " +
        "FROM events WHERE aggregate_id = ? AND event_version >= ? ORDER BY event_version ASC";
    
    private static final String COUNT_EVENTS_SQL = 
        "SELECT COUNT(*) FROM events WHERE aggregate_id = ?";
    
    private static final String MAX_VERSION_SQL = 
        "SELECT MAX(event_version) FROM events WHERE aggregate_id = ?";
    
    public NativeEventStore(DataSource dataSource, EventSerializer eventSerializer) {
        this.dataSource = dataSource;
        this.eventSerializer = eventSerializer;
        this.eventProcessingExecutor = Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "event-processor");
            t.setDaemon(true);
            return t;
        });
        this.dbOperationExecutor = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "db-operation");
            t.setDaemon(true);
            return t;
        });
        initializeSchema();
    }
    
    @Override
    public void saveEvent(UUID aggregateId, DomainEvent event) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_EVENT_SQL)) {
            
            long version = getNextVersion(aggregateId);
            String serializedEvent = eventSerializer.serialize(event);
            
            stmt.setString(1, aggregateId.toString());
            stmt.setString(2, event.getClass().getSimpleName());
            stmt.setString(3, serializedEvent);
            stmt.setLong(4, version);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(6, event.getMetadata().getCorrelationId().toString());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Failed to save event for aggregate " + aggregateId);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error saving event for aggregate " + aggregateId, e);
        }
    }
    
    @Override
    public CompletableFuture<Void> saveEventAsync(UUID aggregateId, DomainEvent event) {
        return CompletableFuture.runAsync(() -> {
            saveEvent(aggregateId, event);
        }, dbOperationExecutor);
    }
    
    @Override
    public List<DomainEvent> loadEvents(UUID aggregateId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_EVENTS_SQL)) {
            
            stmt.setString(1, aggregateId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<DomainEvent> events = new ArrayList<>();
                while (rs.next()) {
                    DomainEvent event = deserializeEvent(rs);
                    events.add(event);
                }
                return events;
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error loading events for aggregate " + aggregateId, e);
        }
    }
    
    @Override
    public CompletableFuture<List<DomainEvent>> loadEventsAsync(UUID aggregateId) {
        return CompletableFuture.supplyAsync(() -> {
            return loadEvents(aggregateId);
        }, dbOperationExecutor);
    }
    
    @Override
    public List<DomainEvent> loadEventsFromVersion(UUID aggregateId, long fromVersion) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_EVENTS_FROM_VERSION_SQL)) {
            
            stmt.setString(1, aggregateId.toString());
            stmt.setLong(2, fromVersion);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<DomainEvent> events = new ArrayList<>();
                while (rs.next()) {
                    DomainEvent event = deserializeEvent(rs);
                    events.add(event);
                }
                return events;
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error loading events from version " + fromVersion + 
                                     " for aggregate " + aggregateId, e);
        }
    }
    
    @Override
    public CompletableFuture<List<DomainEvent>> loadEventsFromVersionAsync(UUID aggregateId, long fromVersion) {
        return CompletableFuture.supplyAsync(() -> {
            return loadEventsFromVersion(aggregateId, fromVersion);
        }, dbOperationExecutor);
    }
    
    @Override
    public EventPage loadEvents(UUID aggregateId, int offset, int limit) {
        try (Connection conn = dataSource.getConnection()) {
            
            // Get total count
            long totalEvents = getEventCount(aggregateId);
            
            // Get paginated events
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_EVENTS_PAGINATED_SQL)) {
                stmt.setString(1, aggregateId.toString());
                stmt.setInt(2, limit);
                stmt.setInt(3, offset);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<DomainEvent> events = new ArrayList<>();
                    while (rs.next()) {
                        DomainEvent event = deserializeEvent(rs);
                        events.add(event);
                    }
                    
                    return new EventPage(events, offset, limit, totalEvents);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error loading paginated events for aggregate " + aggregateId, e);
        }
    }
    
    @Override
    public CompletableFuture<EventPage> loadEventsAsync(UUID aggregateId, int offset, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            return loadEvents(aggregateId, offset, limit);
        }, dbOperationExecutor);
    }
    
    @Override
    public boolean hasEvents(UUID aggregateId) {
        return getEventCount(aggregateId) > 0;
    }
    
    @Override
    public long getEventCount(UUID aggregateId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT_EVENTS_SQL)) {
            
            stmt.setString(1, aggregateId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error counting events for aggregate " + aggregateId, e);
        }
    }
    
    @Override
    public long getLatestVersion(UUID aggregateId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(MAX_VERSION_SQL)) {
            
            stmt.setString(1, aggregateId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long version = rs.getLong(1);
                    return rs.wasNull() ? 0 : version;
                }
                return 0;
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting latest version for aggregate " + aggregateId, e);
        }
    }
    
    private long getNextVersion(UUID aggregateId) {
        return versionCounters.computeIfAbsent(aggregateId, k -> new AtomicLong(getLatestVersion(aggregateId)))
                             .incrementAndGet();
    }
    
    private DomainEvent deserializeEvent(ResultSet rs) throws SQLException {
        String eventType = rs.getString("event_type");
        String eventData = rs.getString("event_data");
        return eventSerializer.deserialize(eventData, eventType);
    }
    
    private void initializeSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create events table if it doesn't exist
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS events (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    aggregate_id VARCHAR(36) NOT NULL,
                    event_type VARCHAR(255) NOT NULL,
                    event_data CLOB NOT NULL,
                    event_version BIGINT NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    correlation_id VARCHAR(36)
                )
                """;
            
            stmt.executeUpdate(createTableSQL);
            
            // Create indexes separately for H2 compatibility
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_aggregate_id ON events(aggregate_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_aggregate_version ON events(aggregate_id, event_version)");
            stmt.executeUpdate("ALTER TABLE events ADD CONSTRAINT IF NOT EXISTS uk_aggregate_version UNIQUE (aggregate_id, event_version)");
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize event store schema", e);
        }
    }
}