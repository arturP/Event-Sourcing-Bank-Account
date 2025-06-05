package io.artur.eventsourcing.aggregates;

import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.MoneyDepositedEvent;
import io.artur.eventsourcing.events.MoneyWithdrawnEvent;
import io.artur.eventsourcing.events.AccountOpenedEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.eventstores.SnapshotCapable;
import io.artur.eventsourcing.exceptions.OverdraftExceededException;
import io.artur.eventsourcing.snapshots.AccountSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BankAccount {

    private static final int MAX_TRANSACTIONS_BEFORE_SNAPSHOT = 10;
    private UUID accountId;
    private String accountHolder;
    private BigDecimal balance;
    private BigDecimal overdraftLimit;
    private EventStore<AccountEvent, UUID> eventStore;
    private AccountSnapshot latestSnapshot;

    public BankAccount(final  EventStore<AccountEvent, UUID> eventStore) {
        this.eventStore = eventStore;
        this.balance = BigDecimal.ZERO;
        this.overdraftLimit = BigDecimal.ZERO;
    }

    public void apply(AccountEvent event) {
        if (event instanceof AccountOpenedEvent openEvent) {
            if (!eventStore.isEmpty(event.getId())) {
                throw new IllegalStateException("Account already opened");
            }
            this.accountId = openEvent.getId();
            this.accountHolder = openEvent.getAccountHolder();
            this.overdraftLimit = openEvent.getOverdraftLimit();
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
        openAccount(accountHolder, BigDecimal.ZERO);
    }
    
    public void openAccount(final String accountHolder, final BigDecimal overdraftLimit) {
        apply(new AccountOpenedEvent(UUID.randomUUID(), accountHolder, overdraftLimit));
    }

    public BigDecimal deposit(final BigDecimal amount) {
        apply(new MoneyDepositedEvent(this.accountId, amount));
        createSnapshotIfNeeded();
        return balance;
    }

    public BigDecimal withdraw(final BigDecimal amount) {
        BigDecimal newBalance = this.balance.subtract(amount);
        BigDecimal minimumAllowedBalance = this.overdraftLimit.negate();
        
        if (newBalance.compareTo(minimumAllowedBalance) < 0) {
            throw new OverdraftExceededException(this.balance, this.overdraftLimit, amount);
        }
        
        apply(new MoneyWithdrawnEvent(this.accountId, amount));
        createSnapshotIfNeeded();
        return balance;
    }

    private void createSnapshotIfNeeded() {
        if (MAX_TRANSACTIONS_BEFORE_SNAPSHOT <= eventStore.eventsCount(accountId)) {
            this.latestSnapshot = new AccountSnapshot(this);
            if (eventStore instanceof SnapshotCapable snapshotCapable) {
                snapshotCapable.saveSnapshot(this.latestSnapshot);
            }
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
    
    public BigDecimal getOverdraftLimit() {
        return this.overdraftLimit;
    }
    
    public void setOverdraftLimit(final BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
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
            bankAccount.overdraftLimit = snapshot.getOverdraftLimit();
            bankAccount.latestSnapshot = snapshot;

            eventsToApply = eventsToApply.stream()
                    .filter(event -> event.getTimestamp().isAfter(snapshot.getSnapshotTime()))
                    .toList();
        }

        eventsToApply.forEach(bankAccount::apply);

        return bankAccount;
    }
    
    public static BankAccount loadFromStore(final EventStore<AccountEvent, UUID> eventStore, final UUID accountId) {
        AccountSnapshot snapshot = null;
        
        // Try to load snapshot if the event store supports it
        if (eventStore instanceof SnapshotCapable snapshotCapable) {
            Optional<AccountSnapshot> optionalSnapshot = snapshotCapable.getLatestSnapshot(accountId);
            snapshot = optionalSnapshot.orElse(null);
        }
        
        // Get the event stream
        List<AccountEvent> events = eventStore.getEventStream(accountId);
        
        if (events.isEmpty() && snapshot == null) {
            throw new IllegalArgumentException("Account with ID " + accountId + " does not exist");
        }
        
        return reconstruct(eventStore, events, snapshot);
    }
}
