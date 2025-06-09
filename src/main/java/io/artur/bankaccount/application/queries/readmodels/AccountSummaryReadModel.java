package io.artur.bankaccount.application.queries.readmodels;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class AccountSummaryReadModel {
    
    private UUID accountId;
    private String accountNumber;
    private String accountHolderName;
    private BigDecimal balance;
    private BigDecimal overdraftLimit;
    private BigDecimal availableBalance;
    private String accountStatus;
    private LocalDateTime lastTransactionDate;
    private LocalDateTime accountOpenedDate;
    private LocalDateTime lastStatusChange;
    private String statusChangedBy;
    private String statusChangeReason;
    private long totalTransactions;
    private long version;
    
    public AccountSummaryReadModel() {}
    
    public AccountSummaryReadModel(UUID accountId, String accountNumber, String accountHolderName,
                                  BigDecimal balance, BigDecimal overdraftLimit, String accountStatus,
                                  LocalDateTime accountOpenedDate) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.overdraftLimit = overdraftLimit != null ? overdraftLimit : BigDecimal.ZERO;
        this.availableBalance = this.balance.add(this.overdraftLimit);
        this.accountStatus = accountStatus != null ? accountStatus : "ACTIVE";
        this.accountOpenedDate = accountOpenedDate;
        this.lastStatusChange = accountOpenedDate;
        this.totalTransactions = 0;
        this.version = 1;
    }
    
    // Factory methods for common scenarios
    public static AccountSummaryReadModel fromAccountOpened(UUID accountId, String accountNumber, 
                                                           String accountHolderName, BigDecimal overdraftLimit) {
        return new AccountSummaryReadModel(accountId, accountNumber, accountHolderName, 
                                         BigDecimal.ZERO, overdraftLimit, "ACTIVE", LocalDateTime.now());
    }
    
    // Business methods
    public void updateBalance(BigDecimal newBalance) {
        this.balance = newBalance;
        this.availableBalance = this.balance.add(this.overdraftLimit);
        this.lastTransactionDate = LocalDateTime.now();
        this.totalTransactions++;
        this.version++;
    }
    
    public void updateStatus(String newStatus, String changedBy, String reason) {
        this.accountStatus = newStatus;
        this.lastStatusChange = LocalDateTime.now();
        this.statusChangedBy = changedBy;
        this.statusChangeReason = reason;
        this.version++;
    }
    
    public void incrementTransactionCount() {
        this.totalTransactions++;
        this.lastTransactionDate = LocalDateTime.now();
        this.version++;
    }
    
    // Getters and setters
    public UUID getAccountId() {
        return accountId;
    }
    
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
        if (this.overdraftLimit != null) {
            this.availableBalance = this.balance.add(this.overdraftLimit);
        }
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
        if (this.balance != null) {
            this.availableBalance = this.balance.add(this.overdraftLimit);
        }
    }
    
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
    
    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }
    
    public String getAccountStatus() {
        return accountStatus;
    }
    
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
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
    
    public LocalDateTime getLastStatusChange() {
        return lastStatusChange;
    }
    
    public void setLastStatusChange(LocalDateTime lastStatusChange) {
        this.lastStatusChange = lastStatusChange;
    }
    
    public String getStatusChangedBy() {
        return statusChangedBy;
    }
    
    public void setStatusChangedBy(String statusChangedBy) {
        this.statusChangedBy = statusChangedBy;
    }
    
    public String getStatusChangeReason() {
        return statusChangeReason;
    }
    
    public void setStatusChangeReason(String statusChangeReason) {
        this.statusChangeReason = statusChangeReason;
    }
    
    public long getTotalTransactions() {
        return totalTransactions;
    }
    
    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }
    
    public long getVersion() {
        return version;
    }
    
    public void setVersion(long version) {
        this.version = version;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountSummaryReadModel that = (AccountSummaryReadModel) o;
        return Objects.equals(accountId, that.accountId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }
    
    @Override
    public String toString() {
        return String.format("AccountSummaryReadModel{accountId=%s, accountHolderName='%s', balance=%s, status='%s'}", 
                           accountId, accountHolderName, balance, accountStatus);
    }
}