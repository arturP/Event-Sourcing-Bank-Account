package io.artur.bankaccount.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.artur.bankaccount.application.queries.readmodels.TransactionReadModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionHistoryResponse {
    
    private UUID transactionId;
    private UUID accountId;
    private String transactionType;
    private BigDecimal amount;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private BigDecimal balanceAfter;
    private String status;
    private UUID relatedAccountId;
    private String metadata;
    
    public TransactionHistoryResponse() {}
    
    public TransactionHistoryResponse(UUID transactionId, UUID accountId, String transactionType,
                                     BigDecimal amount, String description, LocalDateTime timestamp,
                                     BigDecimal balanceAfter, String status, UUID relatedAccountId, String metadata) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.balanceAfter = balanceAfter;
        this.status = status;
        this.relatedAccountId = relatedAccountId;
        this.metadata = metadata;
    }
    
    public static TransactionHistoryResponse fromReadModel(TransactionReadModel readModel) {
        return new TransactionHistoryResponse(
            readModel.getTransactionId(),
            readModel.getAccountId(),
            readModel.getTransactionType(),
            readModel.getAmount(),
            readModel.getDescription(),
            readModel.getTimestamp(),
            readModel.getBalanceAfter(),
            readModel.getStatus(),
            readModel.getRelatedAccountId(),
            readModel.getMetadata()
        );
    }
    
    // Getters and setters
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public UUID getRelatedAccountId() { return relatedAccountId; }
    public void setRelatedAccountId(UUID relatedAccountId) { this.relatedAccountId = relatedAccountId; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}