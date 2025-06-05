package io.artur.eventsourcing.eventstores;

import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.AccountOpenedEvent;
import io.artur.eventsourcing.events.MoneyDepositedEvent;
import io.artur.eventsourcing.snapshots.AccountSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JdbcEventStoreTest {

    private JdbcEventStore eventStore;
    private UUID testAccountId;
    private final String testAccountHolder = "Test User";

    @BeforeEach
    void setUp() throws SQLException {
        // Use H2 in-memory database for testing
        String jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
        String username = "sa";
        String password = "";
        
        // Create a new event store
        eventStore = new JdbcEventStore(jdbcUrl, username, password);
        
        // Create a random account ID for testing
        testAccountId = UUID.randomUUID();
        
        // Clear database tables
        try (Connection conn = eventStore.dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // We'll try to delete but wrap in try/catch since tables might not exist yet
            try {
                stmt.execute("DELETE FROM account_events");
            } catch (SQLException e) {
                // Ignore - table might not exist yet
            }
            
            try {
                stmt.execute("DELETE FROM account_snapshots");
            } catch (SQLException e) {
                // Ignore - table might not exist yet
            }
        }
    }

    @Test
    void testSaveAndRetrieveEvent() {
        // Create a test event
        AccountOpenedEvent event = new AccountOpenedEvent(testAccountId, testAccountHolder);
        
        // Save the event
        eventStore.saveEvent(testAccountId, event);
        
        // Retrieve the event stream
        List<AccountEvent> events = eventStore.getEventStream(testAccountId);
        
        // Verify
        assertFalse(events.isEmpty());
        assertEquals(1, events.size());
        assertEquals(testAccountId, events.get(0).getId());
        assertTrue(events.get(0) instanceof AccountOpenedEvent);
        assertEquals(testAccountHolder, ((AccountOpenedEvent) events.get(0)).getAccountHolder());
    }

    @Test
    void testEventsCount() {
        // Create and save multiple events
        AccountOpenedEvent openEvent = new AccountOpenedEvent(testAccountId, testAccountHolder);
        MoneyDepositedEvent depositEvent = new MoneyDepositedEvent(testAccountId, BigDecimal.TEN);
        
        // Save events
        eventStore.saveEvent(testAccountId, openEvent);
        eventStore.saveEvent(testAccountId, depositEvent);
        
        // Verify count
        assertEquals(2, eventStore.eventsCount(testAccountId));
    }

    @Test
    void testIsEmpty() {
        // Initially the stream should be empty
        assertTrue(eventStore.isEmpty(testAccountId));
        
        // After adding an event, it should not be empty
        AccountOpenedEvent event = new AccountOpenedEvent(testAccountId, testAccountHolder);
        eventStore.saveEvent(testAccountId, event);
        
        assertFalse(eventStore.isEmpty(testAccountId));
    }
    
    @Test
    void testSaveAndRetrieveSnapshot() {
        // Create a test snapshot
        UUID accountId = UUID.randomUUID();
        String accountHolder = "Test User";
        BigDecimal balance = BigDecimal.valueOf(100);
        LocalDateTime snapshotTime = LocalDateTime.now();
        
        AccountSnapshot snapshot = new AccountSnapshot(accountId, accountHolder, balance, snapshotTime);
        
        // Save snapshot
        eventStore.saveSnapshot(snapshot);
        
        // Retrieve snapshot
        Optional<AccountSnapshot> retrieved = eventStore.getLatestSnapshot(accountId);
        
        // Verify
        assertTrue(retrieved.isPresent());
        assertEquals(accountId, retrieved.get().getAccountId());
        assertEquals(accountHolder, retrieved.get().getAccountHolder());
        assertEquals(balance, retrieved.get().getBalance());
    }
}