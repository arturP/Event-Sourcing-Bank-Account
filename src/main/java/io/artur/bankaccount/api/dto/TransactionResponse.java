package io.artur.bankaccount.api.dto;

import java.math.BigDecimal;

public class TransactionResponse {
    private String status;
    private String message;
    private BigDecimal amount;
    
    public TransactionResponse() {}
    
    public TransactionResponse(String status, String message, BigDecimal amount) {
        this.status = status;
        this.message = message;
        this.amount = amount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}