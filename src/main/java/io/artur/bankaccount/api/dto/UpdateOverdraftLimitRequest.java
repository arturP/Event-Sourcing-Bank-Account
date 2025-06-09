package io.artur.bankaccount.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class UpdateOverdraftLimitRequest {
    
    @NotNull(message = "New overdraft limit is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Overdraft limit must be non-negative")
    @DecimalMax(value = "100000.0", inclusive = true, message = "Overdraft limit cannot exceed 100,000")
    @Digits(integer = 8, fraction = 2, message = "Overdraft limit must have at most 8 integer digits and 2 decimal places")
    private BigDecimal newOverdraftLimit;
    
    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;
    
    public UpdateOverdraftLimitRequest() {}
    
    public UpdateOverdraftLimitRequest(BigDecimal newOverdraftLimit, String reason) {
        this.newOverdraftLimit = newOverdraftLimit;
        this.reason = reason;
    }
    
    public BigDecimal getNewOverdraftLimit() {
        return newOverdraftLimit;
    }
    
    public void setNewOverdraftLimit(BigDecimal newOverdraftLimit) {
        this.newOverdraftLimit = newOverdraftLimit;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}