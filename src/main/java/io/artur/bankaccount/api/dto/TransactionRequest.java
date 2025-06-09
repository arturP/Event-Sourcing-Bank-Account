package io.artur.bankaccount.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransactionRequest {
    
    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Transaction amount must be at least 0.01")
    @DecimalMax(value = "1000000.0", message = "Transaction amount cannot exceed 1,000,000")
    @Digits(integer = 10, fraction = 2, message = "Transaction amount must have at most 10 integer digits and 2 decimal places")
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