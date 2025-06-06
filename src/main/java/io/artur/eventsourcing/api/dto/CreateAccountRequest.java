package io.artur.eventsourcing.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateAccountRequest {
    
    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    private String accountHolder;
    
    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance cannot be negative")
    @DecimalMax(value = "1000000.0", message = "Initial balance cannot exceed 1,000,000")
    private BigDecimal initialBalance;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Overdraft limit cannot be negative")
    @DecimalMax(value = "10000.0", message = "Overdraft limit cannot exceed 10,000")
    private BigDecimal overdraftLimit = BigDecimal.ZERO;
    
    public CreateAccountRequest() {}
    
    public CreateAccountRequest(String accountHolder, BigDecimal initialBalance, BigDecimal overdraftLimit) {
        this.accountHolder = accountHolder;
        this.initialBalance = initialBalance;
        this.overdraftLimit = overdraftLimit;
    }
    
    public String getAccountHolder() {
        return accountHolder;
    }
    
    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }
    
    public BigDecimal getInitialBalance() {
        return initialBalance;
    }
    
    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
}