package io.artur.eventsourcing.snapshots;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcSnapshotStore implements SnapshotStore {
    
    private static final Logger LOGGER = Logger.getLogger(JdbcSnapshotStore.class.getName());
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public JdbcSnapshotStore(DataSource dataSource) {
        this.dataSource = dataSource;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create snapshots table if it doesn't exist
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS account_snapshots (
                    snapshot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    account_id UUID NOT NULL,
                    snapshot_time TIMESTAMP NOT NULL,
                    snapshot_data CLOB NOT NULL
                )
            """);
            
            // Create unique constraint
            stmt.execute("""
                ALTER TABLE account_snapshots ADD CONSTRAINT IF NOT EXISTS
                unique_account_snapshot UNIQUE (account_id, snapshot_time)
            """);
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize snapshot store database", e);
            throw new RuntimeException("Failed to initialize snapshot store database", e);
        }
    }

    @Override
    public void saveSnapshot(AccountSnapshot snapshot) {
        String sql = "INSERT INTO account_snapshots (account_id, snapshot_time, snapshot_data) VALUES (?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String snapshotJson = serializeSnapshot(snapshot);
            
            pstmt.setObject(1, snapshot.getAccountId());
            pstmt.setTimestamp(2, Timestamp.valueOf(snapshot.getSnapshotTime()));
            pstmt.setString(3, snapshotJson);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save snapshot", e);
            throw new RuntimeException("Failed to save snapshot", e);
        }
    }

    @Override
    public Optional<AccountSnapshot> getLatestSnapshot(UUID accountId) {
        String sql = "SELECT snapshot_data FROM account_snapshots " +
                     "WHERE account_id = ? " +
                     "ORDER BY snapshot_time DESC LIMIT 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, accountId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String snapshotJson = rs.getString("snapshot_data");
                    return Optional.of(deserializeSnapshot(snapshotJson));
                } else {
                    return Optional.empty();
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve snapshot", e);
            throw new RuntimeException("Failed to retrieve snapshot", e);
        }
    }

    @Override
    public void deleteSnapshots(UUID accountId) {
        String sql = "DELETE FROM account_snapshots WHERE account_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, accountId);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete snapshots", e);
            throw new RuntimeException("Failed to delete snapshots", e);
        }
    }

    private String serializeSnapshot(AccountSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(new SerializableSnapshot(
                snapshot.getAccountId(),
                snapshot.getAccountHolder(),
                snapshot.getBalance(),
                snapshot.getSnapshotTime()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize snapshot", e);
        }
    }

    private AccountSnapshot deserializeSnapshot(String json) {
        try {
            SerializableSnapshot serializableSnapshot = objectMapper.readValue(json, SerializableSnapshot.class);
            return new SerializableSnapshot(
                    serializableSnapshot.getAccountId(),
                    serializableSnapshot.getAccountHolder(),
                    serializableSnapshot.getBalance(),
                    serializableSnapshot.getSnapshotTime()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize snapshot", e);
        }
    }

    // Static class for JSON serialization/deserialization
    private static class SerializableSnapshot extends AccountSnapshot {
        // Constructor needed for Jackson deserialization
        private SerializableSnapshot() {
            super(null, null, null, null);
        }

        public SerializableSnapshot(UUID accountId, String accountHolder, java.math.BigDecimal balance, LocalDateTime snapshotTime) {
            super(accountId, accountHolder, balance, snapshotTime);
        }
    }
}