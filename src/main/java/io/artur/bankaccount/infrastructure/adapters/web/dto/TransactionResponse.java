package io.artur.bankaccount.infrastructure.adapters.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionResponse {
    
    private UUID accountId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal newBalance;
    private String description;
    private LocalDateTime timestamp;
    private boolean successful;
    private String message;
    
    public TransactionResponse() {}
    
    public TransactionResponse(UUID accountId, String transactionType, BigDecimal amount, 
                             BigDecimal newBalance, String description, LocalDateTime timestamp, 
                             boolean successful, String message) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.newBalance = newBalance;
        this.description = description;
        this.timestamp = timestamp;
        this.successful = successful;
        this.message = message;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public BigDecimal getNewBalance() {
        return newBalance;
    }
    
    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}