package io.artur.bankaccount.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.artur.bankaccount.application.ports.outgoing.TransactionHistoryQueryRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionStatisticsResponse {
    
    private long totalTransactions;
    private long depositCount;
    private long withdrawalCount;
    private long transferInCount;
    private long transferOutCount;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalTransferIn;
    private BigDecimal totalTransferOut;
    private BigDecimal averageTransactionAmount;
    private BigDecimal largestTransaction;
    private BigDecimal smallestTransaction;
    private BigDecimal netFlow;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstTransactionDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTransactionDate;
    
    public TransactionStatisticsResponse() {}
    
    public TransactionStatisticsResponse(long totalTransactions, long depositCount, long withdrawalCount,
                                       long transferInCount, long transferOutCount, BigDecimal totalDeposits,
                                       BigDecimal totalWithdrawals, BigDecimal totalTransferIn, BigDecimal totalTransferOut,
                                       BigDecimal averageTransactionAmount, BigDecimal largestTransaction,
                                       BigDecimal smallestTransaction, BigDecimal netFlow, LocalDateTime firstTransactionDate,
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
        this.netFlow = netFlow;
        this.firstTransactionDate = firstTransactionDate;
        this.lastTransactionDate = lastTransactionDate;
    }
    
    public static TransactionStatisticsResponse fromStatistics(TransactionHistoryQueryRepository.TransactionStatistics stats) {
        return new TransactionStatisticsResponse(
            stats.getTotalTransactions(),
            stats.getDepositCount(),
            stats.getWithdrawalCount(),
            stats.getTransferInCount(),
            stats.getTransferOutCount(),
            stats.getTotalDeposits(),
            stats.getTotalWithdrawals(),
            stats.getTotalTransferIn(),
            stats.getTotalTransferOut(),
            stats.getAverageTransactionAmount(),
            stats.getLargestTransaction(),
            stats.getSmallestTransaction(),
            stats.getNetFlow(),
            stats.getFirstTransactionDate(),
            stats.getLastTransactionDate()
        );
    }
    
    // Getters and setters
    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }
    
    public long getDepositCount() { return depositCount; }
    public void setDepositCount(long depositCount) { this.depositCount = depositCount; }
    
    public long getWithdrawalCount() { return withdrawalCount; }
    public void setWithdrawalCount(long withdrawalCount) { this.withdrawalCount = withdrawalCount; }
    
    public long getTransferInCount() { return transferInCount; }
    public void setTransferInCount(long transferInCount) { this.transferInCount = transferInCount; }
    
    public long getTransferOutCount() { return transferOutCount; }
    public void setTransferOutCount(long transferOutCount) { this.transferOutCount = transferOutCount; }
    
    public BigDecimal getTotalDeposits() { return totalDeposits; }
    public void setTotalDeposits(BigDecimal totalDeposits) { this.totalDeposits = totalDeposits; }
    
    public BigDecimal getTotalWithdrawals() { return totalWithdrawals; }
    public void setTotalWithdrawals(BigDecimal totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }
    
    public BigDecimal getTotalTransferIn() { return totalTransferIn; }
    public void setTotalTransferIn(BigDecimal totalTransferIn) { this.totalTransferIn = totalTransferIn; }
    
    public BigDecimal getTotalTransferOut() { return totalTransferOut; }
    public void setTotalTransferOut(BigDecimal totalTransferOut) { this.totalTransferOut = totalTransferOut; }
    
    public BigDecimal getAverageTransactionAmount() { return averageTransactionAmount; }
    public void setAverageTransactionAmount(BigDecimal averageTransactionAmount) { this.averageTransactionAmount = averageTransactionAmount; }
    
    public BigDecimal getLargestTransaction() { return largestTransaction; }
    public void setLargestTransaction(BigDecimal largestTransaction) { this.largestTransaction = largestTransaction; }
    
    public BigDecimal getSmallestTransaction() { return smallestTransaction; }
    public void setSmallestTransaction(BigDecimal smallestTransaction) { this.smallestTransaction = smallestTransaction; }
    
    public BigDecimal getNetFlow() { return netFlow; }
    public void setNetFlow(BigDecimal netFlow) { this.netFlow = netFlow; }
    
    public LocalDateTime getFirstTransactionDate() { return firstTransactionDate; }
    public void setFirstTransactionDate(LocalDateTime firstTransactionDate) { this.firstTransactionDate = firstTransactionDate; }
    
    public LocalDateTime getLastTransactionDate() { return lastTransactionDate; }
    public void setLastTransactionDate(LocalDateTime lastTransactionDate) { this.lastTransactionDate = lastTransactionDate; }
}