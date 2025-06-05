package io.artur.eventsourcing.domain.services;

import io.artur.eventsourcing.domain.AccountHolder;
import io.artur.eventsourcing.domain.AccountNumber;
import io.artur.eventsourcing.domain.Money;
import io.artur.eventsourcing.repository.BankAccountRepository;

import java.util.ArrayList;
import java.util.List;

public class AccountValidationService {
    
    private final BankAccountRepository repository;
    private final Money minimumOpeningBalance;
    private final Money maximumOverdraftLimit;
    
    public AccountValidationService(BankAccountRepository repository) {
        this(repository, Money.zero(), Money.of(5000.0));
    }
    
    public AccountValidationService(BankAccountRepository repository, Money minimumOpeningBalance, Money maximumOverdraftLimit) {
        this.repository = repository;
        this.minimumOpeningBalance = minimumOpeningBalance;
        this.maximumOverdraftLimit = maximumOverdraftLimit;
    }
    
    public ValidationResult validateAccountCreation(AccountNumber accountNumber, AccountHolder accountHolder, Money overdraftLimit) {
        List<String> errors = new ArrayList<>();
        
        // Validate account number uniqueness
        if (repository.findByAccountNumber(accountNumber).isPresent()) {
            errors.add("Account number already exists: " + accountNumber);
        }
        
        // Validate overdraft limit
        if (overdraftLimit.isNegative()) {
            errors.add("Overdraft limit cannot be negative");
        }
        
        if (overdraftLimit.isGreaterThan(maximumOverdraftLimit)) {
            errors.add("Overdraft limit exceeds maximum allowed: " + maximumOverdraftLimit);
        }
        
        // Validate account holder
        if (accountHolder == null) {
            errors.add("Account holder cannot be null");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    public ValidationResult validateDeposit(Money amount) {
        List<String> errors = new ArrayList<>();
        
        if (amount == null) {
            errors.add("Deposit amount cannot be null");
        } else if (amount.isNegative() || amount.isZero()) {
            errors.add("Deposit amount must be positive");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    public ValidationResult validateWithdrawal(Money amount, Money currentBalance, Money overdraftLimit) {
        List<String> errors = new ArrayList<>();
        
        if (amount == null) {
            errors.add("Withdrawal amount cannot be null");
        } else if (amount.isNegative() || amount.isZero()) {
            errors.add("Withdrawal amount must be positive");
        } else {
            Money newBalance = currentBalance.subtract(amount);
            Money minimumAllowedBalance = overdraftLimit.negate();
            
            if (newBalance.isLessThan(minimumAllowedBalance)) {
                errors.add("Withdrawal would exceed overdraft limit. Available: " + 
                          currentBalance.add(overdraftLimit) + ", Requested: " + amount);
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    public ValidationResult validateOverdraftLimitChange(Money newLimit, Money currentBalance) {
        List<String> errors = new ArrayList<>();
        
        if (newLimit == null) {
            errors.add("Overdraft limit cannot be null");
        } else if (newLimit.isNegative()) {
            errors.add("Overdraft limit cannot be negative");
        } else if (newLimit.isGreaterThan(maximumOverdraftLimit)) {
            errors.add("Overdraft limit exceeds maximum allowed: " + maximumOverdraftLimit);
        } else if (currentBalance.isNegative()) {
            // If account is in overdraft, new limit must be at least the current overdraft amount
            Money currentOverdraft = currentBalance.negate();
            if (newLimit.isLessThan(currentOverdraft)) {
                errors.add("Cannot reduce overdraft limit below current overdraft amount: " + currentOverdraft);
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
        
        public void throwIfInvalid() {
            if (!valid) {
                throw new IllegalArgumentException(getErrorMessage());
            }
        }
    }
}