package io.artur.eventsourcing.domain.services;

import io.artur.eventsourcing.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverdraftServiceTest {

    private OverdraftService overdraftService;

    @BeforeEach
    void setUp() {
        overdraftService = new OverdraftService();
    }

    @Test
    void canWithdrawWithinBalance() {
        Money currentBalance = Money.of(100.0);
        Money overdraftLimit = Money.of(50.0);
        Money withdrawalAmount = Money.of(80.0);
        
        assertTrue(overdraftService.canWithdraw(currentBalance, overdraftLimit, withdrawalAmount));
    }

    @Test
    void canWithdrawWithinOverdraftLimit() {
        Money currentBalance = Money.of(100.0);
        Money overdraftLimit = Money.of(50.0);
        Money withdrawalAmount = Money.of(140.0); // Uses $40 of overdraft
        
        assertTrue(overdraftService.canWithdraw(currentBalance, overdraftLimit, withdrawalAmount));
    }

    @Test
    void cannotWithdrawExceedingOverdraftLimit() {
        Money currentBalance = Money.of(100.0);
        Money overdraftLimit = Money.of(50.0);
        Money withdrawalAmount = Money.of(160.0); // Would exceed overdraft by $10
        
        assertFalse(overdraftService.canWithdraw(currentBalance, overdraftLimit, withdrawalAmount));
    }

    @Test
    void calculateOverdraftFee() {
        Money negativeBalance = Money.of(-50.0);
        Money overdraftLimit = Money.of(100.0);
        
        Money fee = overdraftService.calculateOverdraftFee(negativeBalance, overdraftLimit);
        
        assertEquals(Money.of(35.0), fee);
    }

    @Test
    void noOverdraftFeeForPositiveBalance() {
        Money positiveBalance = Money.of(50.0);
        Money overdraftLimit = Money.of(100.0);
        
        Money fee = overdraftService.calculateOverdraftFee(positiveBalance, overdraftLimit);
        
        assertEquals(Money.zero(), fee);
    }

    @Test
    void calculateDailyOverdraftInterest() {
        Money overdraftAmount = Money.of(100.0);
        
        Money dailyInterest = overdraftService.calculateDailyOverdraftInterest(overdraftAmount);
        
        // 15% annual rate / 365 days * $100 = approximately $0.041
        assertTrue(dailyInterest.isPositive());
        assertTrue(dailyInterest.isLessThan(Money.of(1.0)));
    }

    @Test
    void getMaximumWithdrawalAmount() {
        Money currentBalance = Money.of(100.0);
        Money overdraftLimit = Money.of(50.0);
        
        Money maxWithdrawal = overdraftService.getMaximumWithdrawalAmount(currentBalance, overdraftLimit);
        
        assertEquals(Money.of(150.0), maxWithdrawal);
    }

    @Test
    void isInOverdraft() {
        assertTrue(overdraftService.isInOverdraft(Money.of(-50.0)));
        assertFalse(overdraftService.isInOverdraft(Money.of(50.0)));
        assertFalse(overdraftService.isInOverdraft(Money.zero()));
    }

    @Test
    void getOverdraftAmount() {
        Money negativeBalance = Money.of(-75.0);
        Money positiveBalance = Money.of(50.0);
        
        assertEquals(Money.of(75.0), overdraftService.getOverdraftAmount(negativeBalance));
        assertEquals(Money.zero(), overdraftService.getOverdraftAmount(positiveBalance));
    }

    @Test
    void analyzeOverdraftSituation() {
        Money currentBalance = Money.of(-25.0); // $25 in overdraft
        Money overdraftLimit = Money.of(100.0);
        
        OverdraftService.OverdraftAnalysis analysis = 
                overdraftService.analyzeOverdraftSituation(currentBalance, overdraftLimit);
        
        assertTrue(analysis.isInOverdraft());
        assertEquals(Money.of(25.0), analysis.getOverdraftAmount());
        assertEquals(Money.of(75.0), analysis.getAvailableOverdraft());
        assertEquals(Money.of(75.0), analysis.getMaximumWithdrawal());
        assertTrue(analysis.getDailyInterest().isPositive());
        assertEquals(Money.of(35.0), analysis.getOverdraftFee());
    }

    @Test
    void analyzeNonOverdraftSituation() {
        Money currentBalance = Money.of(150.0);
        Money overdraftLimit = Money.of(100.0);
        
        OverdraftService.OverdraftAnalysis analysis = 
                overdraftService.analyzeOverdraftSituation(currentBalance, overdraftLimit);
        
        assertFalse(analysis.isInOverdraft());
        assertEquals(Money.zero(), analysis.getOverdraftAmount());
        assertEquals(Money.of(100.0), analysis.getAvailableOverdraft());
        assertEquals(Money.of(250.0), analysis.getMaximumWithdrawal());
        assertEquals(Money.zero(), analysis.getDailyInterest());
        assertEquals(Money.zero(), analysis.getOverdraftFee());
    }
}