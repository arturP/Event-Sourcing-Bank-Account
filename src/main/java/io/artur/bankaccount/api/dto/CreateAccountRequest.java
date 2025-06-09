package io.artur.bankaccount.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateAccountRequest {
    
    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-'.]+$", message = "Account holder name can only contain letters, spaces, hyphens, apostrophes, and periods")
    private String accountHolderName;
    
    @NotNull(message = "Overdraft limit is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Overdraft limit must be non-negative")
    @DecimalMax(value = "100000.0", inclusive = true, message = "Overdraft limit cannot exceed 100,000")
    @Digits(integer = 8, fraction = 2, message = "Overdraft limit must have at most 8 integer digits and 2 decimal places")
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