package io.artur.bankaccount.domain.account.aggregates;

import io.artur.bankaccount.domain.account.events.*;
import io.artur.bankaccount.domain.account.exceptions.OverdraftExceededException;
import io.artur.bankaccount.domain.account.valueobjects.AccountHolder;
import io.artur.bankaccount.domain.account.valueobjects.AccountNumber;
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
        
        MoneyDepositedEvent event = new MoneyDepositedEvent(this.accountId, amount, metadata);
        apply(event);
    }

    public void withdraw(BigDecimal amount, EventMetadata metadata) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
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
        } else if (event instanceof MoneyDepositedEvent depositEvent) {
            this.balance = this.balance.add(Money.of(depositEvent.getAmount()));
        } else if (event instanceof MoneyWithdrawnEvent withdrawEvent) {
            this.balance = this.balance.subtract(Money.of(withdrawEvent.getAmount()));
        } else if (event instanceof MoneyTransferredEvent transferEvent) {
            this.balance = this.balance.subtract(Money.of(transferEvent.getAmount()));
        } else if (event instanceof MoneyReceivedEvent receivedEvent) {
            this.balance = this.balance.add(Money.of(receivedEvent.getAmount()));
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
}