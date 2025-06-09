package io.artur.bankaccount.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.artur.bankaccount.application.queries.readmodels.AccountSummaryReadModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountSummaryResponse {
    
    private UUID accountId;
    private String accountNumber;
    private String accountHolderName;
    private BigDecimal balance;
    private BigDecimal overdraftLimit;
    private BigDecimal availableBalance;
    private String accountStatus;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTransactionDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime accountOpenedDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastStatusChange;
    
    private String statusChangedBy;
    private String statusChangeReason;
    private long totalTransactions;
    private long version;
    
    public AccountSummaryResponse() {}
    
    public AccountSummaryResponse(UUID accountId, String accountNumber, String accountHolderName,
                                 BigDecimal balance, BigDecimal overdraftLimit, BigDecimal availableBalance,
                                 String accountStatus, LocalDateTime lastTransactionDate, LocalDateTime accountOpenedDate,
                                 LocalDateTime lastStatusChange, String statusChangedBy, String statusChangeReason,
                                 long totalTransactions, long version) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = balance;
        this.overdraftLimit = overdraftLimit;
        this.availableBalance = availableBalance;
        this.accountStatus = accountStatus;
        this.lastTransactionDate = lastTransactionDate;
        this.accountOpenedDate = accountOpenedDate;
        this.lastStatusChange = lastStatusChange;
        this.statusChangedBy = statusChangedBy;
        this.statusChangeReason = statusChangeReason;
        this.totalTransactions = totalTransactions;
        this.version = version;
    }
    
    public static AccountSummaryResponse fromReadModel(AccountSummaryReadModel readModel) {
        return new AccountSummaryResponse(
            readModel.getAccountId(),
            readModel.getAccountNumber(),
            readModel.getAccountHolderName(),
            readModel.getBalance(),
            readModel.getOverdraftLimit(),
            readModel.getAvailableBalance(),
            readModel.getAccountStatus(),
            readModel.getLastTransactionDate(),
            readModel.getAccountOpenedDate(),
            readModel.getLastStatusChange(),
            readModel.getStatusChangedBy(),
            readModel.getStatusChangeReason(),
            readModel.getTotalTransactions(),
            readModel.getVersion()
        );
    }
    
    // Getters and setters
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public void setOverdraftLimit(BigDecimal overdraftLimit) { this.overdraftLimit = overdraftLimit; }
    
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    
    public LocalDateTime getLastTransactionDate() { return lastTransactionDate; }
    public void setLastTransactionDate(LocalDateTime lastTransactionDate) { this.lastTransactionDate = lastTransactionDate; }
    
    public LocalDateTime getAccountOpenedDate() { return accountOpenedDate; }
    public void setAccountOpenedDate(LocalDateTime accountOpenedDate) { this.accountOpenedDate = accountOpenedDate; }
    
    public LocalDateTime getLastStatusChange() { return lastStatusChange; }
    public void setLastStatusChange(LocalDateTime lastStatusChange) { this.lastStatusChange = lastStatusChange; }
    
    public String getStatusChangedBy() { return statusChangedBy; }
    public void setStatusChangedBy(String statusChangedBy) { this.statusChangedBy = statusChangedBy; }
    
    public String getStatusChangeReason() { return statusChangeReason; }
    public void setStatusChangeReason(String statusChangeReason) { this.statusChangeReason = statusChangeReason; }
    
    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }
    
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}