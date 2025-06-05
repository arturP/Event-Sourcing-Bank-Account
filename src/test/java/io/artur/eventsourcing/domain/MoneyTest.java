package io.artur.eventsourcing.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void createMoneyWithValidAmount() {
        Money money = Money.of(100.50);
        assertEquals(new BigDecimal("100.50"), money.getAmount());
        assertEquals(Currency.getInstance("USD"), money.getCurrency());
    }

    @Test
    void createMoneyFromString() {
        Money money = Money.of("250.75");
        assertEquals(new BigDecimal("250.75"), money.getAmount());
    }

    @Test
    void createZeroMoney() {
        Money zero = Money.zero();
        assertTrue(zero.isZero());
        assertFalse(zero.isPositive());
        assertFalse(zero.isNegative());
    }

    @Test
    void addMoney() {
        Money money1 = Money.of(100.0);
        Money money2 = Money.of(50.0);
        Money result = money1.add(money2);
        
        assertEquals(Money.of(150.0), result);
    }

    @Test
    void subtractMoney() {
        Money money1 = Money.of(100.0);
        Money money2 = Money.of(30.0);
        Money result = money1.subtract(money2);
        
        assertEquals(Money.of(70.0), result);
    }

    @Test
    void multiplyMoney() {
        Money money = Money.of(100.0);
        Money result = money.multiply(1.5);
        
        assertEquals(Money.of(150.0), result);
    }

    @Test
    void negateMoney() {
        Money money = Money.of(100.0);
        Money result = money.negate();
        
        assertEquals(Money.of(-100.0), result);
        assertTrue(result.isNegative());
    }

    @Test
    void compareMoney() {
        Money money1 = Money.of(100.0);
        Money money2 = Money.of(50.0);
        Money money3 = Money.of(100.0);
        
        assertTrue(money1.isGreaterThan(money2));
        assertTrue(money2.isLessThan(money1));
        assertTrue(money1.isGreaterThanOrEqual(money3));
        assertTrue(money1.isLessThanOrEqual(money3));
    }

    @Test
    void throwsExceptionForDifferentCurrencies() {
        Money usd = new Money(BigDecimal.valueOf(100), Currency.getInstance("USD"));
        Money eur = new Money(BigDecimal.valueOf(100), Currency.getInstance("EUR"));
        
        assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
    }

    @Test
    void throwsExceptionForNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money((BigDecimal) null));
    }

    @Test
    void moneyEqualityAndHashCode() {
        Money money1 = Money.of(100.0);
        Money money2 = Money.of(100.0);
        Money money3 = Money.of(200.0);
        
        assertEquals(money1, money2);
        assertNotEquals(money1, money3);
        assertEquals(money1.hashCode(), money2.hashCode());
    }

    @Test
    void moneyToString() {
        Money money = Money.of(100.50);
        String result = money.toString();
        
        assertTrue(result.contains("100.50"));
    }
}