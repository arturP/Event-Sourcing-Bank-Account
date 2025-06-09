package io.artur.bankaccount.application.queries.readmodels;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class TransactionReadModel {
    
    private UUID transactionId;
    private UUID accountId;
    private String transactionType;
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;
    private BigDecimal balanceAfter;
    private String status;
    private UUID relatedAccountId; // For transfers
    private String metadata;
    
    public TransactionReadModel() {}
    
    public TransactionReadModel(UUID transactionId, UUID accountId, String transactionType,
                               BigDecimal amount, String description, LocalDateTime timestamp,
                               BigDecimal balanceAfter, String status) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.balanceAfter = balanceAfter;
        this.status = status;
    }
    
    // Factory methods for different transaction types
    public static TransactionReadModel createDeposit(UUID accountId, BigDecimal amount, String description,
                                                    LocalDateTime timestamp, BigDecimal balanceAfter) {
        return new TransactionReadModel(UUID.randomUUID(), accountId, "DEPOSIT", amount, 
                                      description, timestamp, balanceAfter, "COMPLETED");
    }
    
    public static TransactionReadModel createWithdrawal(UUID accountId, BigDecimal amount, String description,
                                                       LocalDateTime timestamp, BigDecimal balanceAfter) {
        return new TransactionReadModel(UUID.randomUUID(), accountId, "WITHDRAWAL", amount, 
                                      description, timestamp, balanceAfter, "COMPLETED");
    }
    
    public static TransactionReadModel createTransferOut(UUID accountId, UUID toAccountId, BigDecimal amount, 
                                                        String description, LocalDateTime timestamp, BigDecimal balanceAfter) {
        TransactionReadModel transaction = new TransactionReadModel(UUID.randomUUID(), accountId, "TRANSFER_OUT", 
                                                                   amount, description, timestamp, balanceAfter, "COMPLETED");
        transaction.setRelatedAccountId(toAccountId);
        return transaction;
    }
    
    public static TransactionReadModel createTransferIn(UUID accountId, UUID fromAccountId, BigDecimal amount, 
                                                       String description, LocalDateTime timestamp, BigDecimal balanceAfter) {
        TransactionReadModel transaction = new TransactionReadModel(UUID.randomUUID(), accountId, "TRANSFER_IN", 
                                                                   amount, description, timestamp, balanceAfter, "COMPLETED");
        transaction.setRelatedAccountId(fromAccountId);
        return transaction;
    }
    
    // Business methods
    public boolean isCredit() {
        return "DEPOSIT".equals(transactionType) || "TRANSFER_IN".equals(transactionType);
    }
    
    public boolean isDebit() {
        return "WITHDRAWAL".equals(transactionType) || "TRANSFER_OUT".equals(transactionType);
    }
    
    public boolean isTransfer() {
        return "TRANSFER_IN".equals(transactionType) || "TRANSFER_OUT".equals(transactionType);
    }
    
    // Getters and setters
    public UUID getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
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
    
    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }
    
    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public UUID getRelatedAccountId() {
        return relatedAccountId;
    }
    
    public void setRelatedAccountId(UUID relatedAccountId) {
        this.relatedAccountId = relatedAccountId;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionReadModel that = (TransactionReadModel) o;
        return Objects.equals(transactionId, that.transactionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
    
    @Override
    public String toString() {
        return String.format("TransactionReadModel{transactionId=%s, accountId=%s, type='%s', amount=%s, timestamp=%s}", 
                           transactionId, accountId, transactionType, amount, timestamp);
    }
}