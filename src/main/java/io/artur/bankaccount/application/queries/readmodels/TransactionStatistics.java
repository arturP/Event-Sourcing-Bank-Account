package io.artur.bankaccount.application.queries.readmodels;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TransactionStatistics {
    private final long totalTransactions;
    private final long depositCount;
    private final long withdrawalCount;
    private final long transferInCount;
    private final long transferOutCount;
    private final BigDecimal totalDeposits;
    private final BigDecimal totalWithdrawals;
    private final BigDecimal totalTransferIn;
    private final BigDecimal totalTransferOut;
    private final BigDecimal averageTransactionAmount;
    private final BigDecimal largestTransaction;
    private final BigDecimal smallestTransaction;
    private final LocalDateTime firstTransactionDate;
    private final LocalDateTime lastTransactionDate;

    public TransactionStatistics(long totalTransactions, long depositCount, long withdrawalCount,
                               long transferInCount, long transferOutCount, BigDecimal totalDeposits,
                               BigDecimal totalWithdrawals, BigDecimal totalTransferIn, BigDecimal totalTransferOut,
                               BigDecimal averageTransactionAmount, BigDecimal largestTransaction,
                               BigDecimal smallestTransaction, LocalDateTime firstTransactionDate,
                               LocalDateTime lastTransactionDate) {
        this.totalTransactions = totalTransactions;
        this.depositCount = depositCount;
        this.withdrawalCount = withdrawalCount;
        this.transferInCount = transferInCount;
        this.transferOutCount = transferOutCount;
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.totalTransferIn = totalTransferIn;
        this.totalTransferOut = totalTransferOut;
        this.averageTransactionAmount = averageTransactionAmount;
        this.largestTransaction = largestTransaction;
        this.smallestTransaction = smallestTransaction;
        this.firstTransactionDate = firstTransactionDate;
        this.lastTransactionDate = lastTransactionDate;
    }

    // Getters
    public long getTotalTransactions() { return totalTransactions; }
    public long getDepositCount() { return depositCount; }
    public long getWithdrawalCount() { return withdrawalCount; }
    public long getTransferInCount() { return transferInCount; }
    public long getTransferOutCount() { return transferOutCount; }
    public BigDecimal getTotalDeposits() { return totalDeposits; }
    public BigDecimal getTotalWithdrawals() { return totalWithdrawals; }
    public BigDecimal getTotalTransferIn() { return totalTransferIn; }
    public BigDecimal getTotalTransferOut() { return totalTransferOut; }
    public BigDecimal getAverageTransactionAmount() { return averageTransactionAmount; }
    public BigDecimal getLargestTransaction() { return largestTransaction; }
    public BigDecimal getSmallestTransaction() { return smallestTransaction; }
    public LocalDateTime getFirstTransactionDate() { return firstTransactionDate; }
    public LocalDateTime getLastTransactionDate() { return lastTransactionDate; }

    public BigDecimal getNetFlow() {
        return totalDeposits.add(totalTransferIn).subtract(totalWithdrawals).subtract(totalTransferOut);
    }
}
