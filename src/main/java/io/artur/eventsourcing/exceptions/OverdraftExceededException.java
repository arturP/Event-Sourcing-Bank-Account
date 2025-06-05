package io.artur.eventsourcing.exceptions;

import java.math.BigDecimal;

public class OverdraftExceededException extends RuntimeException {
    
    private final BigDecimal currentBalance;
    private final BigDecimal overdraftLimit;
    private final BigDecimal requestedAmount;
    
    public OverdraftExceededException(BigDecimal currentBalance, BigDecimal overdraftLimit, BigDecimal requestedAmount) {
        super(String.format("Withdrawal of %s would exceed overdraft limit. Current balance: %s, Overdraft limit: %s", 
                requestedAmount, currentBalance, overdraftLimit));
        this.currentBalance = currentBalance;
        this.overdraftLimit = overdraftLimit;
        this.requestedAmount = requestedAmount;
    }
    
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}