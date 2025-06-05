package io.artur.eventsourcing.projections;

import io.artur.eventsourcing.events.AccountOpenedEvent;
import io.artur.eventsourcing.events.MoneyDepositedEvent;
import io.artur.eventsourcing.events.MoneyWithdrawnEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountSummaryProjectionTest {

    private AccountSummaryProjectionHandler handler;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        handler = new AccountSummaryProjectionHandler();
        accountId = UUID.randomUUID();
    }

    @Test
    void handleAccountOpenedEvent() {
        AccountOpenedEvent event = new AccountOpenedEvent(accountId, "John Doe", BigDecimal.valueOf(200));
        
        handler.handle(event);
        AccountSummaryProjection projection = handler.getProjection();
        
        assertEquals(accountId, projection.getAccountId());
        assertEquals("John Doe", projection.getAccountHolder());
        assertEquals(BigDecimal.valueOf(200), projection.getOverdraftLimit());
        assertEquals(BigDecimal.ZERO, projection.getCurrentBalance());
        assertEquals(0, projection.getTransactionCount());
        assertEquals(event.getTimestamp(), projection.getAccountOpenedDate());
        assertEquals(event.getTimestamp(), projection.getLastTransactionDate());
    }

    @Test
    void handleMoneyDepositedEvent() {
        // First open an account
        handler.handle(new AccountOpenedEvent(accountId, "John Doe", BigDecimal.valueOf(100)));
        
        MoneyDepositedEvent depositEvent = new MoneyDepositedEvent(accountId, BigDecimal.valueOf(500));
        handler.handle(depositEvent);
        
        AccountSummaryProjection projection = handler.getProjection();
        
        assertEquals(BigDecimal.valueOf(500), projection.getCurrentBalance());
        assertEquals(1, projection.getTransactionCount());
        assertEquals(depositEvent.getTimestamp(), projection.getLastTransactionDate());
    }

    @Test
    void handleMoneyWithdrawnEvent() {
        // First open an account and add money
        handler.handle(new AccountOpenedEvent(accountId, "John Doe", BigDecimal.valueOf(100)));
        handler.handle(new MoneyDepositedEvent(accountId, BigDecimal.valueOf(500)));
        
        MoneyWithdrawnEvent withdrawEvent = new MoneyWithdrawnEvent(accountId, BigDecimal.valueOf(150));
        handler.handle(withdrawEvent);
        
        AccountSummaryProjection projection = handler.getProjection();
        
        assertEquals(BigDecimal.valueOf(350), projection.getCurrentBalance());
        assertEquals(2, projection.getTransactionCount());
        assertEquals(withdrawEvent.getTimestamp(), projection.getLastTransactionDate());
    }

    @Test
    void handleMultipleTransactions() {
        // Open account
        handler.handle(new AccountOpenedEvent(accountId, "Alice Smith", BigDecimal.valueOf(50)));
        
        // Multiple transactions
        handler.handle(new MoneyDepositedEvent(accountId, BigDecimal.valueOf(1000)));
        handler.handle(new MoneyWithdrawnEvent(accountId, BigDecimal.valueOf(200)));
        handler.handle(new MoneyDepositedEvent(accountId, BigDecimal.valueOf(300)));
        handler.handle(new MoneyWithdrawnEvent(accountId, BigDecimal.valueOf(50)));
        
        AccountSummaryProjection projection = handler.getProjection();
        
        assertEquals("Alice Smith", projection.getAccountHolder());
        assertEquals(BigDecimal.valueOf(1050), projection.getCurrentBalance()); // 1000 - 200 + 300 - 50
        assertEquals(4, projection.getTransactionCount());
        assertEquals(BigDecimal.valueOf(50), projection.getOverdraftLimit());
    }

    @Test
    void resetProjection() {
        // Setup some data
        handler.handle(new AccountOpenedEvent(accountId, "Test User", BigDecimal.valueOf(100)));
        handler.handle(new MoneyDepositedEvent(accountId, BigDecimal.valueOf(500)));
        
        // Verify data exists
        AccountSummaryProjection projection = handler.getProjection();
        assertEquals("Test User", projection.getAccountHolder());
        assertEquals(BigDecimal.valueOf(500), projection.getCurrentBalance());
        
        // Reset and verify it's clean
        handler.reset();
        AccountSummaryProjection resetProjection = handler.getProjection();
        
        assertNull(resetProjection.getAccountId());
        assertNull(resetProjection.getAccountHolder());
        assertEquals(BigDecimal.ZERO, resetProjection.getCurrentBalance());
        assertEquals(0, resetProjection.getTransactionCount());
    }
}