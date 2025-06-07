package io.artur.bankaccount.infrastructure.adapters.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public class TransferRequest {
    
    @NotNull(message = "To account ID is required")
    private UUID toAccountId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "100000.0", message = "Amount cannot exceed 100,000")
    private BigDecimal amount;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    public TransferRequest() {}
    
    public TransferRequest(UUID toAccountId, BigDecimal amount, String description) {
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description;
    }
    
    public UUID getToAccountId() {
        return toAccountId;
    }
    
    public void setToAccountId(UUID toAccountId) {
        this.toAccountId = toAccountId;
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