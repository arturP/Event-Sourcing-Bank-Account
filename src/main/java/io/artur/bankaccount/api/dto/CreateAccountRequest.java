package io.artur.bankaccount.api.dto;

import java.math.BigDecimal;

public class CreateAccountRequest {
    private String accountHolderName;
    private BigDecimal overdraftLimit;
    
    public CreateAccountRequest() {}
    
    public CreateAccountRequest(String accountHolderName, BigDecimal overdraftLimit) {
        this.accountHolderName = accountHolderName;
        this.overdraftLimit = overdraftLimit;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
}