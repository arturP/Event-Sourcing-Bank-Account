package io.artur.eventsourcing.snapshots;

import io.artur.eventsourcing.aggregates.BankAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountSnapshot {

    private final UUID accountId;
    private final String accountHolder;
    private final BigDecimal balance;
    private final LocalDateTime snapshotTime;

    public AccountSnapshot(final BankAccount bankAccount) {
        this.accountId = bankAccount.getAccountId();
        this.accountHolder = bankAccount.getAccountHolder();
        this.balance = bankAccount.getBalance();
        this.snapshotTime = LocalDateTime.now();
    }
    
    public AccountSnapshot(UUID accountId, String accountHolder, BigDecimal balance, LocalDateTime snapshotTime) {
        this.accountId = accountId;
        this.accountHolder = accountHolder;
        this.balance = balance;
        this.snapshotTime = snapshotTime;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getSnapshotTime() {
        return snapshotTime;
    }
}
