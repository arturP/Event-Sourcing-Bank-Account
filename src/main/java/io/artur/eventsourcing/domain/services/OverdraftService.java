package io.artur.eventsourcing.domain.services;

import io.artur.eventsourcing.domain.Money;

public class OverdraftService {
    
    private static final double DEFAULT_OVERDRAFT_INTEREST_RATE = 0.15; // 15% annual
    private static final Money DEFAULT_OVERDRAFT_FEE = Money.of(35.0);
    
    private final double overdraftInterestRate;
    private final Money overdraftFee;
    
    public OverdraftService() {
        this(DEFAULT_OVERDRAFT_INTEREST_RATE, DEFAULT_OVERDRAFT_FEE);
    }
    
    public OverdraftService(double overdraftInterestRate, Money overdraftFee) {
        if (overdraftInterestRate < 0) {
            throw new IllegalArgumentException("Overdraft interest rate cannot be negative");
        }
        if (overdraftFee == null || overdraftFee.isNegative()) {
            throw new IllegalArgumentException("Overdraft fee cannot be null or negative");
        }
        
        this.overdraftInterestRate = overdraftInterestRate;
        this.overdraftFee = overdraftFee;
    }
    
    public boolean canWithdraw(Money currentBalance, Money overdraftLimit, Money withdrawalAmount) {
        if (withdrawalAmount == null || withdrawalAmount.isNegative()) {
            return false;
        }
        
        Money newBalance = currentBalance.subtract(withdrawalAmount);
        Money minimumAllowedBalance = overdraftLimit.negate();
        
        return newBalance.isGreaterThanOrEqual(minimumAllowedBalance);
    }
    
    public Money calculateOverdraftFee(Money currentBalance, Money overdraftLimit) {
        if (currentBalance.isNegative() && overdraftLimit.isPositive()) {
            return overdraftFee;
        }
        return Money.zero();
    }
    
    public Money calculateDailyOverdraftInterest(Money overdraftAmount) {
        if (overdraftAmount.isPositive()) {
            // Daily interest = (annual rate / 365) * overdraft amount
            double dailyRate = overdraftInterestRate / 365.0;
            return overdraftAmount.multiply(dailyRate);
        }
        return Money.zero();
    }
    
    public Money getMaximumWithdrawalAmount(Money currentBalance, Money overdraftLimit) {
        return currentBalance.add(overdraftLimit);
    }
    
    public boolean isInOverdraft(Money currentBalance) {
        return currentBalance.isNegative();
    }
    
    public Money getOverdraftAmount(Money currentBalance) {
        return currentBalance.isNegative() ? currentBalance.negate() : Money.zero();
    }
    
    public OverdraftAnalysis analyzeOverdraftSituation(Money currentBalance, Money overdraftLimit) {
        boolean inOverdraft = isInOverdraft(currentBalance);
        Money overdraftAmount = getOverdraftAmount(currentBalance);
        Money availableOverdraft = inOverdraft ? 
                overdraftLimit.subtract(overdraftAmount) : 
                overdraftLimit;
        Money maxWithdrawal = getMaximumWithdrawalAmount(currentBalance, overdraftLimit);
        Money dailyInterest = calculateDailyOverdraftInterest(overdraftAmount);
        Money overdraftFeeAmount = calculateOverdraftFee(currentBalance, overdraftLimit);
        
        return new OverdraftAnalysis(
                inOverdraft,
                overdraftAmount,
                availableOverdraft,
                maxWithdrawal,
                dailyInterest,
                overdraftFeeAmount
        );
    }
    
    public static class OverdraftAnalysis {
        private final boolean inOverdraft;
        private final Money overdraftAmount;
        private final Money availableOverdraft;
        private final Money maximumWithdrawal;
        private final Money dailyInterest;
        private final Money overdraftFee;
        
        public OverdraftAnalysis(boolean inOverdraft, Money overdraftAmount, Money availableOverdraft,
                               Money maximumWithdrawal, Money dailyInterest, Money overdraftFee) {
            this.inOverdraft = inOverdraft;
            this.overdraftAmount = overdraftAmount;
            this.availableOverdraft = availableOverdraft;
            this.maximumWithdrawal = maximumWithdrawal;
            this.dailyInterest = dailyInterest;
            this.overdraftFee = overdraftFee;
        }
        
        public boolean isInOverdraft() {
            return inOverdraft;
        }
        
        public Money getOverdraftAmount() {
            return overdraftAmount;
        }
        
        public Money getAvailableOverdraft() {
            return availableOverdraft;
        }
        
        public Money getMaximumWithdrawal() {
            return maximumWithdrawal;
        }
        
        public Money getDailyInterest() {
            return dailyInterest;
        }
        
        public Money getOverdraftFee() {
            return overdraftFee;
        }
    }
}