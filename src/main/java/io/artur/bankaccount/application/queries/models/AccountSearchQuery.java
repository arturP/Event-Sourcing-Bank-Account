package io.artur.bankaccount.application.queries.models;

public class AccountSearchQuery {
    
    private final String accountHolderName;
    private final String accountStatus;
    private final java.math.BigDecimal minBalance;
    private final java.math.BigDecimal maxBalance;
    private final int page;
    private final int size;
    private final String sortBy;
    private final String sortDirection;
    
    public AccountSearchQuery(String accountHolderName, String accountStatus, 
                             java.math.BigDecimal minBalance, java.math.BigDecimal maxBalance,
                             int page, int size, String sortBy, String sortDirection) {
        this.accountHolderName = accountHolderName;
        this.accountStatus = accountStatus;
        this.minBalance = minBalance;
        this.maxBalance = maxBalance;
        this.page = Math.max(0, page);
        this.size = Math.min(100, Math.max(1, size));
        this.sortBy = sortBy != null ? sortBy : "accountHolderName";
        this.sortDirection = sortDirection != null ? sortDirection : "ASC";
    }
    
    public static AccountSearchQuery all() {
        return new AccountSearchQuery(null, null, null, null, 0, 20, "accountHolderName", "ASC");
    }
    
    public static AccountSearchQuery byHolderName(String holderName) {
        return new AccountSearchQuery(holderName, null, null, null, 0, 20, "accountHolderName", "ASC");
    }
    
    public static AccountSearchQuery byStatus(String status) {
        return new AccountSearchQuery(null, status, null, null, 0, 20, "accountHolderName", "ASC");
    }
    
    public java.math.BigDecimal getMinBalance() {
        return minBalance;
    }
    
    public java.math.BigDecimal getMaxBalance() {
        return maxBalance;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public String getAccountStatus() {
        return accountStatus;
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
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
}