package io.artur.bankaccount.infrastructure.adapters.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateAccountRequest {
    
    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    private String accountHolder;
    
    @NotNull(message = "Overdraft limit is required")
    @DecimalMin(value = "0.0", message = "Overdraft limit cannot be negative")
    @DecimalMax(value = "10000.0", message = "Overdraft limit cannot exceed 10,000")
    private BigDecimal overdraftLimit;
    
    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
    @DecimalMax(value = "100000.0", message = "Initial balance cannot exceed 100,000")
    private BigDecimal initialBalance;
    
    public CreateAccountRequest() {}
    
    public CreateAccountRequest(String accountHolder, BigDecimal overdraftLimit, BigDecimal initialBalance) {
        this.accountHolder = accountHolder;
        this.overdraftLimit = overdraftLimit;
        this.initialBalance = initialBalance;
    }
    
    public String getAccountHolder() {
        return accountHolder;
    }
    
    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
    
    public BigDecimal getInitialBalance() {
        return initialBalance;
    }
    
    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}