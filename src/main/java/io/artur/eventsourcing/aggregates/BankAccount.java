package io.artur.eventsourcing.aggregates;

import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.MoneyDepositedEvent;
import io.artur.eventsourcing.events.MoneyWithdrawnEvent;
import io.artur.eventsourcing.events.AccountOpenedEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.snapshots.AccountSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BankAccount {

    private static final int MAX_TRANSACTIONS_BEFORE_SNAPSHOT = 10;
    private UUID accountId;
    private String accountHolder;
    private BigDecimal balance;
    private EventStore<AccountEvent, UUID> eventStore;
    private AccountSnapshot latestSnapshot;

    public BankAccount(final  EventStore<AccountEvent, UUID> eventStore) {
        this.eventStore = eventStore;
        this.balance = BigDecimal.ZERO;
    }

    public void apply(AccountEvent event) {
        if (event instanceof AccountOpenedEvent openEvent) {
            if (!eventStore.isEmpty(event.getId())) {
                throw new IllegalStateException("Account already opened");
            }
            this.accountId = openEvent.getId();
            this.accountHolder = openEvent.getAccountHolder();
        } else if (event instanceof MoneyDepositedEvent depositEvent) {
            this.balance = this.balance.add(depositEvent.getAmount());
        } else if (event instanceof MoneyWithdrawnEvent withdrawEvent) {
            this.balance = this.balance.subtract(withdrawEvent.getAmount());
        } else {
            throw new IllegalArgumentException("Unsupported event type " + event.getClass().getName());
        }
        eventStore.saveEvent(event.getId(), event);
    }

    public List<AccountEvent> getEvents() {
        return eventStore.getEventStream(accountId);
    }

    public void openAccount(final String accountHolder) {
        apply(new AccountOpenedEvent(UUID.randomUUID(), accountHolder));
    }

    public BigDecimal deposit(final BigDecimal amount) {
        apply(new MoneyDepositedEvent(this.accountId, amount));
        createSnapshotIfNeeded();
        return balance;
    }

    public BigDecimal withdraw(final BigDecimal amount) {
        if (amount.compareTo(balance) <= 0) {
            apply(new MoneyWithdrawnEvent(this.accountId, amount));
            createSnapshotIfNeeded();
        } else {
            throw new IllegalStateException("Insufficient founds");
        }
        return balance;
    }

    private void createSnapshotIfNeeded() {
        if (MAX_TRANSACTIONS_BEFORE_SNAPSHOT <= eventStore.eventsCount(accountId)) {
            this.latestSnapshot = new AccountSnapshot(this);
        }
    }

    public UUID getAccountId() {
        return this.accountId;
    }

    public String getAccountHolder() {
        return this.accountHolder;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public AccountSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    public static BankAccount reconstruct(
            final EventStore<AccountEvent, UUID> eventStore,
            final List<AccountEvent> eventsToAdd,
            final AccountSnapshot snapshot) {
        final BankAccount bankAccount = new BankAccount(eventStore);
        List<AccountEvent> eventsToApply = new ArrayList<>(eventsToAdd);

        if (snapshot != null) {
            bankAccount.accountId = snapshot.getAccountId();
            bankAccount.accountHolder = snapshot.getAccountHolder();
            bankAccount.balance = snapshot.getBalance();

            eventsToApply = eventsToApply.stream()
                    .filter(event -> event.getTimestamp().isAfter(snapshot.getSnapshotTime()))
                    .toList();
        }

        eventsToApply.forEach(bankAccount::apply);

        return bankAccount;
    }
}
