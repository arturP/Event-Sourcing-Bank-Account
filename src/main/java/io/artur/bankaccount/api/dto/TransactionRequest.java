package io.artur.bankaccount.api.dto;

import java.math.BigDecimal;

public class TransactionRequest {
    private BigDecimal amount;
    private String description;
    
    public TransactionRequest() {}
    
    public TransactionRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
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
}