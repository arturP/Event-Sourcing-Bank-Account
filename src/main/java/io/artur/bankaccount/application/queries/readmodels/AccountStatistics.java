package io.artur.bankaccount.application.queries.readmodels;

import java.math.BigDecimal;

public final class AccountStatistics {
    private final long totalAccounts;
    private final long activeAccounts;
    private final long frozenAccounts;
    private final long closedAccounts;
    private final long dormantAccounts;
    private final BigDecimal totalBalance;
    private final BigDecimal averageBalance;

    public AccountStatistics(long totalAccounts, long activeAccounts, long frozenAccounts,
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

    // Getters
    public long getTotalAccounts() { return totalAccounts; }
    public long getActiveAccounts() { return activeAccounts; }
    public long getFrozenAccounts() { return frozenAccounts; }
    public long getClosedAccounts() { return closedAccounts; }
    public long getDormantAccounts() { return dormantAccounts; }
    public BigDecimal getTotalBalance() { return totalBalance; }
    public BigDecimal getAverageBalance() { return averageBalance; }
}
