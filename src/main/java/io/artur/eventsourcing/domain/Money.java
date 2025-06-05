package io.artur.eventsourcing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public class Money {
    
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.scale() > currency.getDefaultFractionDigits()) {
            throw new IllegalArgumentException("Amount precision exceeds currency fraction digits");
        }
        
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        this.currency = currency;
    }
    
    public Money(BigDecimal amount) {
        this(amount, Currency.getInstance("USD"));
    }
    
    public Money(double amount) {
        this(BigDecimal.valueOf(amount));
    }
    
    public Money(String amount) {
        this(new BigDecimal(amount));
    }
    
    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }
    
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }
    
    public static Money of(double amount) {
        return new Money(amount);
    }
    
    public static Money of(String amount) {
        return new Money(amount);
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public Money multiply(BigDecimal multiplier) {
        BigDecimal result = this.amount.multiply(multiplier)
                .setScale(this.currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return new Money(result, this.currency);
    }
    
    public Money multiply(double multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }
    
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }
    
    public boolean isLessThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot perform operation on different currencies: " + 
                    this.currency + " and " + other.currency);
        }
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return currency.getSymbol() + amount;
    }
}