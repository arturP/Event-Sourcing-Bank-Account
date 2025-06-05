package io.artur.eventsourcing.projections;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountSummaryProjection {
    
    private UUID accountId;
    private String accountHolder;
    private BigDecimal currentBalance;
    private BigDecimal overdraftLimit;
    private int transactionCount;
    private LocalDateTime lastTransactionDate;
    private LocalDateTime accountOpenedDate;
    
    public AccountSummaryProjection() {
        this.currentBalance = BigDecimal.ZERO;
        this.overdraftLimit = BigDecimal.ZERO;
        this.transactionCount = 0;
    }
    
    public AccountSummaryProjection(UUID accountId, String accountHolder, BigDecimal overdraftLimit, LocalDateTime accountOpenedDate) {
        this();
        this.accountId = accountId;
        this.accountHolder = accountHolder;
        this.overdraftLimit = overdraftLimit;
        this.accountOpenedDate = accountOpenedDate;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountHolder() {
        return accountHolder;
    }
    
    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }
    
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
    
    public int getTransactionCount() {
        return transactionCount;
    }
    
    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }
    
    public LocalDateTime getLastTransactionDate() {
        return lastTransactionDate;
    }
    
    public void setLastTransactionDate(LocalDateTime lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }
    
    public LocalDateTime getAccountOpenedDate() {
        return accountOpenedDate;
    }
    
    public void setAccountOpenedDate(LocalDateTime accountOpenedDate) {
        this.accountOpenedDate = accountOpenedDate;
    }
    
    public void incrementTransactionCount() {
        this.transactionCount++;
    }
    
    public void addToBalance(BigDecimal amount) {
        this.currentBalance = this.currentBalance.add(amount);
    }
    
    public void subtractFromBalance(BigDecimal amount) {
        this.currentBalance = this.currentBalance.subtract(amount);
    }
}