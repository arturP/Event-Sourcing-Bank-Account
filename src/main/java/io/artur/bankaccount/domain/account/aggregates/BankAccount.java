package io.artur.bankaccount.domain.account.aggregates;

import io.artur.bankaccount.domain.account.events.*;
import io.artur.bankaccount.domain.account.exceptions.OverdraftExceededException;
import io.artur.bankaccount.domain.account.valueobjects.AccountHolder;
import io.artur.bankaccount.domain.account.valueobjects.AccountNumber;
import io.artur.bankaccount.domain.account.valueobjects.AccountStatus;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import io.artur.bankaccount.domain.shared.valueobjects.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BankAccount {

    private UUID accountId;
    private AccountNumber accountNumber;
    private AccountHolder accountHolder;
    private Money balance;
    private Money overdraftLimit;
    private AccountStatus accountStatus;
    private List<AccountDomainEvent> uncommittedEvents;
    
    public BankAccount() {
        this.balance = Money.zero();
        this.overdraftLimit = Money.zero();
        this.uncommittedEvents = new ArrayList<>();
    }
    
    public BankAccount(UUID accountId) {
        this();
        this.accountId = accountId;
    }

    public static BankAccount openNewAccount(UUID accountId, String accountHolderName, BigDecimal overdraftLimit, EventMetadata metadata) {
        BankAccount account = new BankAccount(accountId);
        
        AccountOpenedEvent event = new AccountOpenedEvent(
            accountId, 
            accountHolderName, 
            overdraftLimit, 
            metadata
        );
        
        account.apply(event);
        return account;
    }
    
    public static BankAccount openNewAccount(String accountHolderName, BigDecimal overdraftLimit, EventMetadata metadata) {
        UUID newAccountId = UUID.randomUUID();
        return openNewAccount(newAccountId, accountHolderName, overdraftLimit, metadata);
    }
    
    public static BankAccount fromHistory(UUID accountId, List<AccountDomainEvent> events) {
        BankAccount account = new BankAccount(accountId);
        for (AccountDomainEvent event : events) {
            account.apply(event, false);
        }
        return account;
    }

    public void deposit(BigDecimal amount, EventMetadata metadata) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        
        validateAccountCanPerformTransactions();
        
        MoneyDepositedEvent event = new MoneyDepositedEvent(this.accountId, amount, metadata);
        apply(event);
    }

    public void withdraw(BigDecimal amount, EventMetadata metadata) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
        validateAccountCanPerformTransactions();
        
        BigDecimal newBalance = this.balance.getAmount().subtract(amount);
        BigDecimal minimumAllowedBalance = this.overdraftLimit.getAmount().negate();
        
        if (newBalance.compareTo(minimumAllowedBalance) < 0) {
            throw new OverdraftExceededException(this.balance.getAmount(), this.overdraftLimit.getAmount(), amount);
        }
        
        MoneyWithdrawnEvent event = new MoneyWithdrawnEvent(this.accountId, amount, metadata);
        apply(event);
    }
    
    public void transferOut(UUID toAccountId, BigDecimal amount, String description, EventMetadata metadata) {
        if (toAccountId == null) {
            throw new IllegalArgumentException("To account ID cannot be null");
        }
        if (this.accountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer money to the same account");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        
        validateAccountCanPerformTransactions();
        
        BigDecimal newBalance = this.balance.getAmount().subtract(amount);
        BigDecimal minimumAllowedBalance = this.overdraftLimit.getAmount().negate();
        
        if (newBalance.compareTo(minimumAllowedBalance) < 0) {
            throw new OverdraftExceededException(this.balance.getAmount(), this.overdraftLimit.getAmount(), amount);
        }
        
        MoneyTransferredEvent event = new MoneyTransferredEvent(this.accountId, toAccountId, amount, description, metadata);
        apply(event);
    }
    
    public void receiveTransfer(UUID fromAccountId, BigDecimal amount, String description, EventMetadata metadata) {
        if (fromAccountId == null) {
            throw new IllegalArgumentException("From account ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        
        MoneyReceivedEvent event = new MoneyReceivedEvent(this.accountId, fromAccountId, amount, description, metadata);
        apply(event);
    }
    
    private void apply(AccountDomainEvent event) {
        apply(event, true);
    }
    
    private void apply(AccountDomainEvent event, boolean isNew) {
        if (event instanceof AccountOpenedEvent openEvent) {
            this.accountId = openEvent.getId();
            this.accountNumber = AccountNumber.generate();
            this.accountHolder = AccountHolder.of(openEvent.getAccountHolder());
            this.overdraftLimit = Money.of(openEvent.getOverdraftLimit());
            this.accountStatus = AccountStatus.createActive("SYSTEM");
        } else if (event instanceof MoneyDepositedEvent depositEvent) {
            this.balance = this.balance.add(Money.of(depositEvent.getAmount()));
        } else if (event instanceof MoneyWithdrawnEvent withdrawEvent) {
            this.balance = this.balance.subtract(Money.of(withdrawEvent.getAmount()));
        } else if (event instanceof MoneyTransferredEvent transferEvent) {
            this.balance = this.balance.subtract(Money.of(transferEvent.getAmount()));
        } else if (event instanceof MoneyReceivedEvent receivedEvent) {
            this.balance = this.balance.add(Money.of(receivedEvent.getAmount()));
        } else if (event instanceof AccountFrozenEvent frozenEvent) {
            this.accountStatus = AccountStatus.createFrozen(frozenEvent.getReason(), frozenEvent.getFrozenBy());
        } else if (event instanceof AccountClosedEvent closedEvent) {
            this.accountStatus = AccountStatus.createClosed(closedEvent.getReason(), closedEvent.getClosedBy());
        } else if (event instanceof AccountReactivatedEvent reactivatedEvent) {
            this.accountStatus = AccountStatus.createActive(reactivatedEvent.getReactivatedBy());
        } else if (event instanceof AccountMarkedDormantEvent dormantEvent) {
            this.accountStatus = AccountStatus.createDormant(dormantEvent.getMarkedBy());
        } else {
            throw new IllegalArgumentException("Unsupported event type " + event.getClass().getName());
        }
        
        if (isNew) {
            uncommittedEvents.add(event);
        }
    }
    
    public List<AccountDomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    public UUID getAccountId() {
        return this.accountId;
    }
    
    public AccountNumber getAccountNumber() {
        return this.accountNumber;
    }

    public AccountHolder getAccountHolder() {
        return this.accountHolder;
    }

    public Money getBalance() {
        return this.balance;
    }
    
    public Money getOverdraftLimit() {
        return this.overdraftLimit;
    }
    
    public void setOverdraftLimit(Money overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
    
    // Account Lifecycle Management Methods
    
    public void freeze(String reason, String frozenBy, EventMetadata metadata) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Freeze reason cannot be null or empty");
        }
        if (frozenBy == null || frozenBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Frozen by cannot be null or empty");
        }
        
        if (this.accountStatus != null && !this.accountStatus.getStatus().canBeFrozen()) {
            throw new IllegalStateException(
                String.format("Cannot freeze account with status %s", this.accountStatus.getStatus())
            );
        }
        
        AccountFrozenEvent event = new AccountFrozenEvent(this.accountId, reason, frozenBy, metadata);
        apply(event);
    }
    
    public void close(String reason, String closedBy, EventMetadata metadata) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Close reason cannot be null or empty");
        }
        if (closedBy == null || closedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Closed by cannot be null or empty");
        }
        
        if (this.accountStatus != null && !this.accountStatus.getStatus().canBeClosed()) {
            throw new IllegalStateException(
                String.format("Cannot close account with status %s", this.accountStatus.getStatus())
            );
        }
        
        AccountClosedEvent event = new AccountClosedEvent(this.accountId, reason, closedBy, this.balance, metadata);
        apply(event);
    }
    
    public void reactivate(String reason, String reactivatedBy, EventMetadata metadata) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reactivation reason cannot be null or empty");
        }
        if (reactivatedBy == null || reactivatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Reactivated by cannot be null or empty");
        }
        
        if (this.accountStatus == null || !this.accountStatus.getStatus().canBeReactivated()) {
            throw new IllegalStateException(
                String.format("Cannot reactivate account with status %s", 
                    this.accountStatus != null ? this.accountStatus.getStatus() : "null")
            );
        }
        
        String previousStatus = this.accountStatus.getStatus().name();
        AccountReactivatedEvent event = new AccountReactivatedEvent(this.accountId, reason, reactivatedBy, previousStatus, metadata);
        apply(event);
    }
    
    public void markDormant(String reason, String markedBy, EventMetadata metadata) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Dormant reason cannot be null or empty");
        }
        if (markedBy == null || markedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Marked by cannot be null or empty");
        }
        
        if (this.accountStatus != null && this.accountStatus.getStatus() == AccountStatus.Status.CLOSED) {
            throw new IllegalStateException("Cannot mark closed account as dormant");
        }
        
        AccountMarkedDormantEvent event = new AccountMarkedDormantEvent(this.accountId, reason, markedBy, null, metadata);
        apply(event);
    }
    
    private void validateAccountCanPerformTransactions() {
        if (this.accountStatus == null || !this.accountStatus.canPerformTransactions()) {
            String status = this.accountStatus != null ? this.accountStatus.getStatus().name() : "UNKNOWN";
            throw new IllegalStateException(
                String.format("Account with status %s cannot perform transactions", status)
            );
        }
    }
    
    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }
}