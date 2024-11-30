package io.artur.eventsourcing.aggregates;

import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.AccountOpenedEvent;
import io.artur.eventsourcing.events.MoneyDepositedEvent;
import io.artur.eventsourcing.snapshots.AccountSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountTest {

    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        bankAccount = new BankAccount();
    }

    @Test
    void openAccountTest() {
        final String accountHolder = "Test User";
        bankAccount.openAccount(accountHolder);

        assertEquals(1, bankAccount.getEvents().size());
        assertEquals(accountHolder, bankAccount.getAccountHolder());
        assertEquals(BigDecimal.ZERO, bankAccount.getBalance());
        assertNotNull(bankAccount.getAccountId());
    }

    @Test
    void performFourOperationsOnAnAccount() {
        final String accountHolder = "Tom Brown";
        final BigDecimal firstDeposit = BigDecimal.valueOf(10.3);
        final BigDecimal firstWithdraw = BigDecimal.valueOf(5.55);
        final BigDecimal secondWithdraw = BigDecimal.valueOf(2.1);
        final BigDecimal secondDeposit = BigDecimal.valueOf(0.21);
        final BigDecimal expectedFinalBalance = BigDecimal.valueOf(2.86);

        bankAccount.openAccount(accountHolder);
        final BigDecimal balanceAfterFirstDeposit = bankAccount.deposit(firstDeposit);
        final BigDecimal balanceAfterFirstWithdraw = bankAccount.withdraw(firstWithdraw);
        final BigDecimal balanceAfterSecondWithdraw = bankAccount.withdraw(secondWithdraw);
        final BigDecimal balanceAfterSecondDeposit = bankAccount.deposit(secondDeposit);

        assertEquals(5, bankAccount.getEvents().size());
        assertEquals(firstDeposit, balanceAfterFirstDeposit);
        assertEquals(BigDecimal.valueOf(4.75), balanceAfterFirstWithdraw);
        assertEquals(BigDecimal.valueOf(2.65), balanceAfterSecondWithdraw);
        assertEquals(expectedFinalBalance, balanceAfterSecondDeposit);
        assertEquals(expectedFinalBalance, bankAccount.getBalance());
        assertNull(bankAccount.getLatestSnapshot());
    }

    @Test
    void checkIfSnapshotCreatedAfterTenEvents() {
        final String accountHolder = "Mark White";
        final BigDecimal deposit = BigDecimal.valueOf(1.01);

        bankAccount.openAccount(accountHolder);
        for (int i = 0; i < 10; i++) {
            bankAccount.deposit(deposit);
        }

        final AccountSnapshot accountSnapshot = bankAccount.getLatestSnapshot();
        assertNotNull(accountSnapshot);
        assertEquals(accountHolder, accountSnapshot.getAccountHolder());
        assertEquals(bankAccount.getAccountId(), accountSnapshot.getAccountId());
        assertEquals(bankAccount.getBalance(), accountSnapshot.getBalance());
        assertNotNull(accountSnapshot.getSnapshotTime());
    }

    @Test
    void restoreFromEventsOnly() {
        final String accountHolder = "Michael Smith";
        final UUID accountId = UUID.randomUUID();
        final BigDecimal deposit = BigDecimal.valueOf(100);
        final List<AccountEvent> accountEvents = List.of(
                new AccountOpenedEvent(accountId, accountHolder),
                new MoneyDepositedEvent(accountId, deposit)
        );

        final BankAccount reconstructed = BankAccount.reconstruct(accountEvents, null);

        assertNotNull(reconstructed);
        assertEquals(accountEvents.size(), reconstructed.getEvents().size());
        assertEquals(accountId, reconstructed.getAccountId());
        assertEquals(deposit, reconstructed.getBalance());
        assertEquals(accountHolder, reconstructed.getAccountHolder());
    }

    @Test
    void restoreFromEventsAndSnapshot() {
        final String accountHolder = "James Williams";
        final UUID accountId = UUID.randomUUID();
        final BigDecimal deposit = BigDecimal.valueOf(200);

        final AccountSnapshot accountSnapshot = createAccountSnapshot(accountId, accountHolder, deposit);
        final List<AccountEvent> events = List.of(
                new MoneyDepositedEvent(accountId, deposit),
                new MoneyDepositedEvent(accountId, deposit)
        );
        final BankAccount reconstructed = BankAccount.reconstruct(events, accountSnapshot);

        assertNotNull(reconstructed);
        assertEquals(events.size(), reconstructed.getEvents().size());
        assertEquals(accountId, reconstructed.getAccountId());
        assertEquals(deposit.multiply(BigDecimal.valueOf(3L)), reconstructed.getBalance());
        assertEquals(accountHolder, reconstructed.getAccountHolder());
    }

    @Test
    void withdrawMoreThanAccountBalance() {
        bankAccount.openAccount("test account");
        bankAccount.deposit(BigDecimal.TEN);
        assertThrows(IllegalStateException.class,
                () -> bankAccount.withdraw(BigDecimal.valueOf(11)));
    }

    @Test
    void applyOpenAccountEventTwoTimes() {
        bankAccount.openAccount("test");
        assertThrows(IllegalStateException.class,
                () -> bankAccount.openAccount("second"));
    }

    @Test
    void applyUnsupportedEventType() {
        final AccountEvent unsupported = new AccountEvent() {
            @Override
            public UUID getId() {
                return null;
            }

            @Override
            public LocalDateTime getTimestamp() {
                return null;
            }
        };
        assertThrows(IllegalArgumentException.class,
                () -> bankAccount.apply(unsupported));
    }

    private AccountSnapshot createAccountSnapshot(
            final UUID accountId,
            final String accountHolder,
            final BigDecimal balance) {
        final BankAccount account = new BankAccount();
        account.apply(new AccountOpenedEvent(accountId, accountHolder));
        account.apply(new MoneyDepositedEvent(accountId, balance));
        return new AccountSnapshot(account);
    }
}