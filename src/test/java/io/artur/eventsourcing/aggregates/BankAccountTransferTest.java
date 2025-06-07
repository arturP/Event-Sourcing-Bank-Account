package io.artur.eventsourcing.aggregates;

import io.artur.eventsourcing.commands.TransferMoneyCommand;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.EventMetadata;
import io.artur.eventsourcing.events.MoneyReceivedEvent;
import io.artur.eventsourcing.events.MoneyTransferredEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.InMemoryEventStore;
import io.artur.eventsourcing.exceptions.OverdraftExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountTransferTest {

    private BankAccount fromAccount;
    private BankAccount toAccount;
    private EventStore<AccountEvent, UUID> eventStore;
    private EventMetadata metadata;

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore<>();
        fromAccount = new BankAccount(eventStore);
        toAccount = new BankAccount(eventStore);
        metadata = new EventMetadata(1);
        
        // Setup accounts
        fromAccount.openAccount("Alice", BigDecimal.valueOf(100), metadata);
        fromAccount.deposit(BigDecimal.valueOf(500), metadata);
        
        toAccount.openAccount("Bob", BigDecimal.ZERO, metadata);
        toAccount.deposit(BigDecimal.valueOf(100), metadata);
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        BigDecimal transferAmount = BigDecimal.valueOf(200);
        BigDecimal fromInitialBalance = fromAccount.getBalance();
        BigDecimal toInitialBalance = toAccount.getBalance();
        
        fromAccount.transferOut(toAccount.getAccountId(), transferAmount, "Test transfer", metadata);
        toAccount.receiveTransfer(fromAccount.getAccountId(), transferAmount, "Test transfer", metadata);
        
        assertEquals(fromInitialBalance.subtract(transferAmount), fromAccount.getBalance());
        assertEquals(toInitialBalance.add(transferAmount), toAccount.getBalance());
    }

    @Test
    void shouldCreateTransferEventsWhenTransferringMoney() {
        BigDecimal transferAmount = BigDecimal.valueOf(150);
        int fromInitialEventCount = fromAccount.getEvents().size();
        int toInitialEventCount = toAccount.getEvents().size();
        
        fromAccount.transferOut(toAccount.getAccountId(), transferAmount, "Event test", metadata);
        toAccount.receiveTransfer(fromAccount.getAccountId(), transferAmount, "Event test", metadata);
        
        assertEquals(fromInitialEventCount + 1, fromAccount.getEvents().size());
        assertEquals(toInitialEventCount + 1, toAccount.getEvents().size());
        
        List<AccountEvent> fromEvents = fromAccount.getEvents();
        List<AccountEvent> toEvents = toAccount.getEvents();
        
        AccountEvent lastFromEvent = fromEvents.get(fromEvents.size() - 1);
        AccountEvent lastToEvent = toEvents.get(toEvents.size() - 1);
        
        assertTrue(lastFromEvent instanceof MoneyTransferredEvent);
        assertTrue(lastToEvent instanceof MoneyReceivedEvent);
        
        MoneyTransferredEvent transferEvent = (MoneyTransferredEvent) lastFromEvent;
        MoneyReceivedEvent receivedEvent = (MoneyReceivedEvent) lastToEvent;
        
        assertEquals(transferAmount, transferEvent.getAmount());
        assertEquals(transferAmount, receivedEvent.getAmount());
        assertEquals(toAccount.getAccountId(), transferEvent.getToAccountId());
        assertEquals(fromAccount.getAccountId(), receivedEvent.getFromAccountId());
        assertEquals("Event test", transferEvent.getDescription());
        assertEquals("Event test", receivedEvent.getDescription());
    }

    @Test
    void shouldThrowExceptionWhenTransferExceedsOverdraftLimit() {
        BigDecimal transferAmount = BigDecimal.valueOf(700); // More than balance + overdraft
        
        assertThrows(OverdraftExceededException.class, () -> {
            fromAccount.transferOut(toAccount.getAccountId(), transferAmount, "Overdraft test", metadata);
        });
    }

    @Test
    void shouldAllowTransferWithinOverdraftLimit() {
        BigDecimal transferAmount = BigDecimal.valueOf(580); // Within balance + overdraft (500 + 100)
        BigDecimal fromInitialBalance = fromAccount.getBalance();
        BigDecimal toInitialBalance = toAccount.getBalance();
        
        assertDoesNotThrow(() -> {
            fromAccount.transferOut(toAccount.getAccountId(), transferAmount, "Overdraft allowed", metadata);
            toAccount.receiveTransfer(fromAccount.getAccountId(), transferAmount, "Overdraft allowed", metadata);
        });
        
        assertEquals(fromInitialBalance.subtract(transferAmount), fromAccount.getBalance());
        assertEquals(toInitialBalance.add(transferAmount), toAccount.getBalance());
        assertTrue(fromAccount.getBalance().compareTo(BigDecimal.ZERO) < 0); // Negative balance
    }
}