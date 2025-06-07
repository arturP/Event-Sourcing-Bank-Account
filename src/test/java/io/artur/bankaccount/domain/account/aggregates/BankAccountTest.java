package io.artur.bankaccount.domain.account.aggregates;

import io.artur.bankaccount.domain.account.events.*;
import io.artur.bankaccount.domain.account.exceptions.OverdraftExceededException;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountTest {

    private EventMetadata metadata;

    @BeforeEach
    void setUp() {
        metadata = new EventMetadata(1);
    }

    @Test
    void shouldOpenNewAccount() {
        String accountHolder = "Test User";
        BigDecimal overdraftLimit = BigDecimal.valueOf(100);
        
        BankAccount account = BankAccount.openNewAccount(accountHolder, overdraftLimit, metadata);
        
        assertNotNull(account.getAccountId());
        assertEquals(accountHolder, account.getAccountHolder().getFullName());
        assertEquals(0, account.getBalance().getAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, overdraftLimit.compareTo(account.getOverdraftLimit().getAmount()));
        
        List<AccountDomainEvent> events = account.getUncommittedEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof AccountOpenedEvent);
    }

    @Test
    void shouldDepositMoney() {
        BankAccount account = BankAccount.openNewAccount("Test User", BigDecimal.valueOf(100), metadata);
        account.markEventsAsCommitted();
        
        BigDecimal depositAmount = BigDecimal.valueOf(50);
        account.deposit(depositAmount, metadata);
        
        assertEquals(0, depositAmount.compareTo(account.getBalance().getAmount()));
        
        List<AccountDomainEvent> events = account.getUncommittedEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof MoneyDepositedEvent);
        assertEquals(depositAmount, ((MoneyDepositedEvent) events.get(0)).getAmount());
    }

    @Test
    void shouldWithdrawMoney() {
        BankAccount account = BankAccount.openNewAccount("Test User", BigDecimal.valueOf(100), metadata);
        account.deposit(BigDecimal.valueOf(200), metadata);
        account.markEventsAsCommitted();
        
        BigDecimal withdrawAmount = BigDecimal.valueOf(50);
        account.withdraw(withdrawAmount, metadata);
        
        assertEquals(0, BigDecimal.valueOf(150).compareTo(account.getBalance().getAmount()));
        
        List<AccountDomainEvent> events = account.getUncommittedEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof MoneyWithdrawnEvent);
        assertEquals(withdrawAmount, ((MoneyWithdrawnEvent) events.get(0)).getAmount());
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        BankAccount fromAccount = BankAccount.openNewAccount("Alice Smith", BigDecimal.valueOf(100), metadata);
        fromAccount.deposit(BigDecimal.valueOf(500), metadata);
        fromAccount.markEventsAsCommitted();
        
        BankAccount toAccount = BankAccount.openNewAccount("Bob Jones", BigDecimal.ZERO, metadata);
        toAccount.deposit(BigDecimal.valueOf(100), metadata);
        toAccount.markEventsAsCommitted();
        
        BigDecimal transferAmount = BigDecimal.valueOf(200);
        String description = "Test transfer";
        
        fromAccount.transferOut(toAccount.getAccountId(), transferAmount, description, metadata);
        toAccount.receiveTransfer(fromAccount.getAccountId(), transferAmount, description, metadata);
        
        assertEquals(0, BigDecimal.valueOf(300).compareTo(fromAccount.getBalance().getAmount()));
        assertEquals(0, BigDecimal.valueOf(300).compareTo(toAccount.getBalance().getAmount()));
        
        List<AccountDomainEvent> fromEvents = fromAccount.getUncommittedEvents();
        List<AccountDomainEvent> toEvents = toAccount.getUncommittedEvents();
        
        assertEquals(1, fromEvents.size());
        assertEquals(1, toEvents.size());
        
        assertTrue(fromEvents.get(0) instanceof MoneyTransferredEvent);
        assertTrue(toEvents.get(0) instanceof MoneyReceivedEvent);
        
        MoneyTransferredEvent transferEvent = (MoneyTransferredEvent) fromEvents.get(0);
        MoneyReceivedEvent receivedEvent = (MoneyReceivedEvent) toEvents.get(0);
        
        assertEquals(transferAmount, transferEvent.getAmount());
        assertEquals(transferAmount, receivedEvent.getAmount());
        assertEquals(description, transferEvent.getDescription());
        assertEquals(description, receivedEvent.getDescription());
    }

    @Test
    void shouldThrowExceptionWhenWithdrawingBeyondOverdraftLimit() {
        BankAccount account = BankAccount.openNewAccount("Test User", BigDecimal.valueOf(100), metadata);
        account.deposit(BigDecimal.valueOf(50), metadata);
        
        BigDecimal withdrawAmount = BigDecimal.valueOf(200); // More than balance + overdraft
        
        assertThrows(OverdraftExceededException.class, () -> {
            account.withdraw(withdrawAmount, metadata);
        });
    }

    @Test
    void shouldAllowWithdrawalWithinOverdraftLimit() {
        BankAccount account = BankAccount.openNewAccount("Test User", BigDecimal.valueOf(100), metadata);
        account.deposit(BigDecimal.valueOf(50), metadata);
        account.markEventsAsCommitted();
        
        BigDecimal withdrawAmount = BigDecimal.valueOf(120); // Within balance + overdraft
        
        assertDoesNotThrow(() -> {
            account.withdraw(withdrawAmount, metadata);
        });
        
        assertEquals(0, BigDecimal.valueOf(-70).compareTo(account.getBalance().getAmount()));
        assertTrue(account.getBalance().isNegative());
    }

    @Test
    void shouldReconstituteFromEventHistory() {
        UUID accountId = UUID.randomUUID();
        List<AccountDomainEvent> events = List.of(
            new AccountOpenedEvent(accountId, "Test User", BigDecimal.valueOf(100), metadata),
            new MoneyDepositedEvent(accountId, BigDecimal.valueOf(200), metadata),
            new MoneyWithdrawnEvent(accountId, BigDecimal.valueOf(50), metadata)
        );
        
        BankAccount account = BankAccount.fromHistory(accountId, events);
        
        assertEquals(accountId, account.getAccountId());
        assertEquals("Test User", account.getAccountHolder().getFullName());
        assertEquals(0, BigDecimal.valueOf(150).compareTo(account.getBalance().getAmount()));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(account.getOverdraftLimit().getAmount()));
        assertTrue(account.getUncommittedEvents().isEmpty());
    }
}