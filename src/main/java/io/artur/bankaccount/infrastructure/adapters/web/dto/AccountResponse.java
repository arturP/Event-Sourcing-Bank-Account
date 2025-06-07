package io.artur.bankaccount.infrastructure.adapters.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountResponse {
    
    private UUID accountId;
    private String accountHolder;
    private BigDecimal balance;
    private BigDecimal overdraftLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public AccountResponse() {}
    
    public AccountResponse(UUID accountId, String accountHolder, BigDecimal balance, 
                          BigDecimal overdraftLimit, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.accountHolder = accountHolder;
        this.balance = balance;
        this.overdraftLimit = overdraftLimit;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}