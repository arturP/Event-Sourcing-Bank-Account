package io.artur.eventsourcing.eventstores;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.serialization.EventSerializer;
import io.artur.eventsourcing.snapshots.AccountSnapshot;
import io.artur.eventsourcing.snapshots.JdbcSnapshotStore;
import io.artur.eventsourcing.snapshots.SnapshotStore;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcEventStore implements EventStore<AccountEvent, UUID>, SnapshotCapable {
    
    private static final Logger LOGGER = Logger.getLogger(JdbcEventStore.class.getName());
    final DataSource dataSource;
    private final EventSerializer eventSerializer;
    private final SnapshotStore snapshotStore;

    public JdbcEventStore(String jdbcUrl, String username, String password, SnapshotStore snapshotStore) {
        this.dataSource = setupDataSource(jdbcUrl, username, password);
        this.eventSerializer = new EventSerializer();
        this.snapshotStore = snapshotStore;
        initializeDatabase();
    }
    
    public JdbcEventStore(String jdbcUrl, String username, String password) {
        this.dataSource = setupDataSource(jdbcUrl, username, password);
        this.eventSerializer = new EventSerializer();
        this.snapshotStore = new JdbcSnapshotStore(this.dataSource);
        initializeDatabase();
    }

    public JdbcEventStore(DataSource dataSource, SnapshotStore snapshotStore) {
        this.dataSource = dataSource;
        this.eventSerializer = new EventSerializer();
        this.snapshotStore = snapshotStore;
        initializeDatabase();
    }
    
    public JdbcEventStore(DataSource dataSource) {
        this.dataSource = dataSource;
        this.eventSerializer = new EventSerializer();
        this.snapshotStore = new JdbcSnapshotStore(this.dataSource);
        initializeDatabase();
    }

    private DataSource setupDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create events table if it doesn't exist
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS account_events (
                    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    account_id UUID NOT NULL,
                    event_timestamp TIMESTAMP NOT NULL,
                    event_type VARCHAR(100) NOT NULL,
                    event_data CLOB NOT NULL
                )
            """);
            
            // Create index separately
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_account_events_account_id ON account_events(account_id)");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    @Override
    public void saveEvent(UUID id, AccountEvent event) {
        String sql = "INSERT INTO account_events (account_id, event_timestamp, event_type, event_data) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String eventJson = eventSerializer.serialize(event);
            String eventType = event.getClass().getSimpleName();
            
            pstmt.setObject(1, id);
            pstmt.setTimestamp(2, Timestamp.valueOf(event.getTimestamp()));
            pstmt.setString(3, eventType);
            pstmt.setString(4, eventJson);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save event", e);
            throw new RuntimeException("Failed to save event", e);
        }
    }

    @Override
    public List<AccountEvent> getEventStream(UUID id) {
        String sql = "SELECT event_data FROM account_events WHERE account_id = ? ORDER BY event_timestamp ASC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<AccountEvent> events = new ArrayList<>();
                
                while (rs.next()) {
                    String eventJson = rs.getString("event_data");
                    AccountEvent event = eventSerializer.deserialize(eventJson);
                    events.add(event);
                }
                
                return events;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve event stream", e);
            throw new RuntimeException("Failed to retrieve event stream", e);
        }
    }

    @Override
    public List<AccountEvent> getEventStream(UUID id, int offset, int limit) {
        String sql = "SELECT event_data FROM account_events WHERE account_id = ? ORDER BY event_timestamp ASC LIMIT ? OFFSET ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, id);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<AccountEvent> events = new ArrayList<>();
                
                while (rs.next()) {
                    String eventJson = rs.getString("event_data");
                    AccountEvent event = eventSerializer.deserialize(eventJson);
                    events.add(event);
                }
                
                return events;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve paginated event stream", e);
            throw new RuntimeException("Failed to retrieve paginated event stream", e);
        }
    }

    @Override
    public boolean isEmpty(UUID id) {
        return eventsCount(id) == 0;
    }

    @Override
    public long eventsCount(UUID id) {
        String sql = "SELECT COUNT(*) FROM account_events WHERE account_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    return 0;
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to count events", e);
            throw new RuntimeException("Failed to count events", e);
        }
    }
    
    @Override
    public void saveSnapshot(AccountSnapshot snapshot) {
        snapshotStore.saveSnapshot(snapshot);
    }
    
    @Override
    public Optional<AccountSnapshot> getLatestSnapshot(UUID accountId) {
        return snapshotStore.getLatestSnapshot(accountId);
    }
}