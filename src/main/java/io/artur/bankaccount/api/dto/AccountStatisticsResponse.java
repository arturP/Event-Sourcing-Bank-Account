package io.artur.bankaccount.api.dto;

import io.artur.bankaccount.application.ports.outgoing.AccountSummaryQueryRepository;

import java.math.BigDecimal;

public class AccountStatisticsResponse {
    
    private long totalAccounts;
    private long activeAccounts;
    private long frozenAccounts;
    private long closedAccounts;
    private long dormantAccounts;
    private BigDecimal totalBalance;
    private BigDecimal averageBalance;
    
    public AccountStatisticsResponse() {}
    
    public AccountStatisticsResponse(long totalAccounts, long activeAccounts, long frozenAccounts,
                                   long closedAccounts, long dormantAccounts, BigDecimal totalBalance,
                                   BigDecimal averageBalance) {
        this.totalAccounts = totalAccounts;
        this.activeAccounts = activeAccounts;
        this.frozenAccounts = frozenAccounts;
        this.closedAccounts = closedAccounts;
        this.dormantAccounts = dormantAccounts;
        this.totalBalance = totalBalance;
        this.averageBalance = averageBalance;
    }
    
    public static AccountStatisticsResponse fromStatistics(AccountSummaryQueryRepository.AccountStatistics stats) {
        return new AccountStatisticsResponse(
            stats.getTotalAccounts(),
            stats.getActiveAccounts(),
            stats.getFrozenAccounts(),
            stats.getClosedAccounts(),
            stats.getDormantAccounts(),
            stats.getTotalBalance(),
            stats.getAverageBalance()
        );
    }
    
    // Getters and setters
    public long getTotalAccounts() { return totalAccounts; }
    public void setTotalAccounts(long totalAccounts) { this.totalAccounts = totalAccounts; }
    
    public long getActiveAccounts() { return activeAccounts; }
    public void setActiveAccounts(long activeAccounts) { this.activeAccounts = activeAccounts; }
    
    public long getFrozenAccounts() { return frozenAccounts; }
    public void setFrozenAccounts(long frozenAccounts) { this.frozenAccounts = frozenAccounts; }
    
    public long getClosedAccounts() { return closedAccounts; }
    public void setClosedAccounts(long closedAccounts) { this.closedAccounts = closedAccounts; }
    
    public long getDormantAccounts() { return dormantAccounts; }
    public void setDormantAccounts(long dormantAccounts) { this.dormantAccounts = dormantAccounts; }
    
    public BigDecimal getTotalBalance() { return totalBalance; }
    public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }
    
    public BigDecimal getAverageBalance() { return averageBalance; }
    public void setAverageBalance(BigDecimal averageBalance) { this.averageBalance = averageBalance; }
}