package io.artur.bankaccount.domain.account.valueobjects;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class AccountNumber {
    
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{8,16}$");
    private final String value;
    
    public AccountNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
        
        String normalizedValue = value.trim().toUpperCase();
        if (!ACCOUNT_NUMBER_PATTERN.matcher(normalizedValue).matches()) {
            throw new IllegalArgumentException("Invalid account number format. Must be 8-16 alphanumeric characters");
        }
        
        this.value = normalizedValue;
    }
    
    public static AccountNumber generate() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return new AccountNumber(uuid.substring(0, 12));
    }
    
    public static AccountNumber of(String value) {
        return new AccountNumber(value);
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDisplayValue() {
        if (value.length() >= 8) {
            return value.substring(0, 4) + "-" + value.substring(4, 8) + 
                   (value.length() > 8 ? "-" + value.substring(8) : "");
        }
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountNumber that = (AccountNumber) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return getDisplayValue();
    }
}