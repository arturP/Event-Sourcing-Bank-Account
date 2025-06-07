package io.artur.eventsourcing.aggregates;

import io.artur.eventsourcing.commands.DepositMoneyCommand;
import io.artur.eventsourcing.commands.OpenAccountCommand;
import io.artur.eventsourcing.commands.TransferMoneyCommand;
import io.artur.eventsourcing.commands.WithdrawMoneyCommand;
import io.artur.eventsourcing.domain.AccountNumber;
import io.artur.eventsourcing.domain.Money;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.EventMetadata;
import io.artur.eventsourcing.events.MoneyDepositedEvent;
import io.artur.eventsourcing.events.MoneyReceivedEvent;
import io.artur.eventsourcing.events.MoneyTransferredEvent;
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
    private AccountNumber accountNumber;
    private String accountHolder;
    private BigDecimal balance;
    private BigDecimal overdraftLimit;
    private final EventStore<AccountEvent, UUID> eventStore;
    private AccountSnapshot latestSnapshot;

    public BankAccount(final  EventStore<AccountEvent, UUID> eventStore) {
        this.eventStore = eventStore;
        this.balance = BigDecimal.ZERO;
        this.overdraftLimit = BigDecimal.ZERO;
    }

    public void apply(AccountEvent event) {
        apply(event, true);
    }
    
    private void apply(AccountEvent event, boolean saveToStore) {
        if (event instanceof AccountOpenedEvent openEvent) {
            if (saveToStore && !eventStore.isEmpty(event.getId())) {
                throw new IllegalStateException("Account already opened");
            }
            this.accountId = openEvent.getId();
            this.accountNumber = AccountNumber.generate();
            this.accountHolder = openEvent.getAccountHolder();
            this.overdraftLimit = openEvent.getOverdraftLimit();
        } else if (event instanceof MoneyDepositedEvent depositEvent) {
            this.balance = this.balance.add(depositEvent.getAmount());
        } else if (event instanceof MoneyWithdrawnEvent withdrawEvent) {
            this.balance = this.balance.subtract(withdrawEvent.getAmount());
        } else if (event instanceof MoneyTransferredEvent transferEvent) {
            this.balance = this.balance.subtract(transferEvent.getAmount());
        } else if (event instanceof MoneyReceivedEvent receivedEvent) {
            this.balance = this.balance.add(receivedEvent.getAmount());
        } else {
            throw new IllegalArgumentException("Unsupported event type " + event.getClass().getName());
        }
        
        if (saveToStore) {
            eventStore.saveEvent(event.getId(), event);
        }
    }

    public List<AccountEvent> getEvents() {
        return eventStore.getEventStream(accountId);
    }

    public void openAccount(final String accountHolder) {
        openAccount(accountHolder, BigDecimal.ZERO);
    }
    
    public void openAccount(final String accountHolder, final BigDecimal overdraftLimit) {
        openAccount(accountHolder, overdraftLimit, new EventMetadata(1));
    }
    
    public void openAccount(final String accountHolder, final BigDecimal overdraftLimit, final EventMetadata metadata) {
        UUID newAccountId = UUID.randomUUID();
        OpenAccountCommand command = new OpenAccountCommand(newAccountId, accountHolder, overdraftLimit, metadata);
        command.validate();
        apply(new AccountOpenedEvent(newAccountId, accountHolder, overdraftLimit, metadata));
    }
    
    public void handle(OpenAccountCommand command) {
        command.validate();
        apply(new AccountOpenedEvent(command.getAggregateId(), command.getAccountHolder(), 
                command.getOverdraftLimit(), command.getMetadata()));
    }

    public BigDecimal deposit(final BigDecimal amount) {
        return deposit(amount, new EventMetadata(1));
    }
    
    public BigDecimal deposit(final BigDecimal amount, final EventMetadata metadata) {
        DepositMoneyCommand command = new DepositMoneyCommand(this.accountId, amount, metadata);
        command.validate();
        apply(new MoneyDepositedEvent(this.accountId, amount, metadata));
        createSnapshotIfNeeded();
        return balance;
    }
    
    public void handle(DepositMoneyCommand command) {
        command.validate();
        apply(new MoneyDepositedEvent(command.getAggregateId(), command.getAmount(), command.getMetadata()));
        createSnapshotIfNeeded();
    }

    public BigDecimal withdraw(final BigDecimal amount) {
        return withdraw(amount, new EventMetadata(1));
    }
    
    public BigDecimal withdraw(final BigDecimal amount, final EventMetadata metadata) {
        WithdrawMoneyCommand command = new WithdrawMoneyCommand(this.accountId, amount, metadata);
        command.validate();
        
        BigDecimal newBalance = this.balance.subtract(amount);
        BigDecimal minimumAllowedBalance = this.overdraftLimit.negate();
        
        if (newBalance.compareTo(minimumAllowedBalance) < 0) {
            throw new OverdraftExceededException(this.balance, this.overdraftLimit, amount);
        }
        
        apply(new MoneyWithdrawnEvent(this.accountId, amount, metadata));
        createSnapshotIfNeeded();
        return balance;
    }
    
    public void handle(WithdrawMoneyCommand command) {
        command.validate();
        
        BigDecimal newBalance = this.balance.subtract(command.getAmount());
        BigDecimal minimumAllowedBalance = this.overdraftLimit.negate();
        
        if (newBalance.compareTo(minimumAllowedBalance) < 0) {
            throw new OverdraftExceededException(this.balance, this.overdraftLimit, command.getAmount());
        }
        
        apply(new MoneyWithdrawnEvent(command.getAggregateId(), command.getAmount(), command.getMetadata()));
        createSnapshotIfNeeded();
    }
    
    public void transferOut(final UUID toAccountId, final BigDecimal amount, final String description, final EventMetadata metadata) {
        TransferMoneyCommand command = new TransferMoneyCommand(this.accountId, toAccountId, amount, description, metadata);
        command.validate();
        
        BigDecimal newBalance = this.balance.subtract(amount);
        BigDecimal minimumAllowedBalance = this.overdraftLimit.negate();
        
        if (newBalance.compareTo(minimumAllowedBalance) < 0) {
            throw new OverdraftExceededException(this.balance, this.overdraftLimit, amount);
        }
        
        apply(new MoneyTransferredEvent(this.accountId, toAccountId, amount, description, metadata));
        createSnapshotIfNeeded();
    }
    
    public void receiveTransfer(final UUID fromAccountId, final BigDecimal amount, final String description, final EventMetadata metadata) {
        apply(new MoneyReceivedEvent(this.accountId, fromAccountId, amount, description, metadata));
        createSnapshotIfNeeded();
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
    
    public AccountNumber getAccountNumber() {
        return this.accountNumber;
    }

    public String getAccountHolder() {
        return this.accountHolder;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }
    
    public Money getBalanceAsMoney() {
        return Money.of(this.balance);
    }
    
    public BigDecimal getOverdraftLimit() {
        return this.overdraftLimit;
    }
    
    public Money getOverdraftLimitAsMoney() {
        return Money.of(this.overdraftLimit);
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

        eventsToApply.forEach(event -> bankAccount.apply(event, false));

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
