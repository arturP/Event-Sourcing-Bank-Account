package io.artur.eventsourcing.replay;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.AccountOpenedEvent;
import io.artur.eventsourcing.events.MoneyDepositedEvent;
import io.artur.eventsourcing.events.MoneyWithdrawnEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.InMemoryEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventReplayServiceTest {

    private EventReplayService replayService;
    private EventStore<AccountEvent, UUID> eventStore;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore<>();
        replayService = new EventReplayService(eventStore);
        accountId = UUID.randomUUID();
        
        // Add some test events
        eventStore.saveEvent(accountId, new AccountOpenedEvent(accountId, "Test User", BigDecimal.valueOf(100)));
        eventStore.saveEvent(accountId, new MoneyDepositedEvent(accountId, BigDecimal.valueOf(500)));
        eventStore.saveEvent(accountId, new MoneyWithdrawnEvent(accountId, BigDecimal.valueOf(150)));
    }

    @Test
    void replayEvents() {
        List<AccountEvent> replayedEvents = new ArrayList<>();
        
        replayService.replayEvents(accountId, replayedEvents::add);
        
        assertEquals(3, replayedEvents.size());
        assertTrue(replayedEvents.get(0) instanceof AccountOpenedEvent);
        assertTrue(replayedEvents.get(1) instanceof MoneyDepositedEvent);
        assertTrue(replayedEvents.get(2) instanceof MoneyWithdrawnEvent);
    }

    @Test
    void replayEventsWithFilter() {
        List<AccountEvent> replayedEvents = new ArrayList<>();
        
        // Filter only money events (deposits and withdrawals)
        replayService.replayEventsWithFilter(accountId, 
                event -> event instanceof MoneyDepositedEvent || event instanceof MoneyWithdrawnEvent,
                replayedEvents::add);
        
        assertEquals(2, replayedEvents.size());
        assertTrue(replayedEvents.get(0) instanceof MoneyDepositedEvent);
        assertTrue(replayedEvents.get(1) instanceof MoneyWithdrawnEvent);
    }

    @Test
    void replayToPointInTime() {
        // Use a future point in time to include all existing events
        LocalDateTime pointInTime = LocalDateTime.now().plusMinutes(1);
        BankAccount account = replayService.replayToPointInTime(accountId, pointInTime);
        
        assertNotNull(account);
        assertEquals("Test User", account.getAccountHolder());
        assertEquals(BigDecimal.valueOf(350), account.getBalance()); // 500 - 150
    }

    @Test
    void debugEventStream() {
        // This test mainly verifies that the debug method doesn't throw exceptions
        assertDoesNotThrow(() -> replayService.debugEventStream(accountId));
    }

    @Test
    void replayNonExistentAccount() {
        UUID nonExistentId = UUID.randomUUID();
        List<AccountEvent> replayedEvents = new ArrayList<>();
        
        replayService.replayEvents(nonExistentId, replayedEvents::add);
        
        assertEquals(0, replayedEvents.size());
    }
}