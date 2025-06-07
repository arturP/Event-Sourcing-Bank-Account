package io.artur.bankaccount.infrastructure.adapters.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransactionRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "100000.0", message = "Amount cannot exceed 100,000")
    private BigDecimal amount;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
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