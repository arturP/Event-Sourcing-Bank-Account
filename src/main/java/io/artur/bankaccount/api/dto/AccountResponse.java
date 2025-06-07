package io.artur.bankaccount.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountResponse {
    private UUID accountId;
    private String accountHolderName;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    
    public AccountResponse() {}
    
    public AccountResponse(UUID accountId, String accountHolderName, BigDecimal balance, BigDecimal availableBalance) {
        this.accountId = accountId;
        this.accountHolderName = accountHolderName;
        this.balance = balance;
        this.availableBalance = availableBalance;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
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
    }
    
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
    
    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }
}