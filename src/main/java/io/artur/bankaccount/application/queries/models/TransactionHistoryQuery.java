package io.artur.bankaccount.application.queries.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionHistoryQuery {
    
    private final UUID accountId;
    private final LocalDateTime fromDate;
    private final LocalDateTime toDate;
    private final String transactionType;
    private final java.math.BigDecimal minAmount;
    private final java.math.BigDecimal maxAmount;
    private final int page;
    private final int size;
    private final String sortBy;
    private final String sortDirection;
    
    public TransactionHistoryQuery(UUID accountId, LocalDateTime fromDate, LocalDateTime toDate, 
                                  String transactionType, java.math.BigDecimal minAmount, java.math.BigDecimal maxAmount,
                                  int page, int size) {
        this.accountId = accountId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.transactionType = transactionType;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.page = Math.max(0, page);
        this.size = Math.min(100, Math.max(1, size));
        this.sortBy = "timestamp";
        this.sortDirection = "DESC";
    }
    
    public static TransactionHistoryQuery forAccount(UUID accountId) {
        return new TransactionHistoryQuery(accountId, null, null, null, null, null, 0, 20);
    }
    
    public static TransactionHistoryQuery forAccountWithPaging(UUID accountId, int page, int size) {
        return new TransactionHistoryQuery(accountId, null, null, null, null, null, page, size);
    }
    
    public java.math.BigDecimal getMinAmount() {
        return minAmount;
    }
    
    public java.math.BigDecimal getMaxAmount() {
        return maxAmount;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public LocalDateTime getFromDate() {
        return fromDate;
    }
    
    public LocalDateTime getToDate() {
        return toDate;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public int getPage() {
        return page;
    }
    
    public int getSize() {
        return size;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public String getSortDirection() {
        return sortDirection;
    }
    
    public void validate() {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
}