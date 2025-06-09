package io.artur.bankaccount.application.ports.outgoing;

import io.artur.bankaccount.application.queries.models.TransactionHistoryQuery;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;
import io.artur.bankaccount.application.queries.readmodels.TransactionReadModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query repository for transaction history read models
 * Provides optimized read operations for transaction data
 */
public interface TransactionHistoryQueryRepository {
    
    /**
     * Find transaction by ID
     */
    Optional<TransactionReadModel> findByTransactionId(UUID transactionId);
    
    /**
     * Get transaction history for an account with pagination and filtering
     */
    PagedResult<TransactionReadModel> getTransactionHistory(TransactionHistoryQuery query);
    
    /**
     * Get recent transactions for an account
     */
    List<TransactionReadModel> getRecentTransactions(UUID accountId, int limit);
    
    /**
     * Find transactions by account and date range
     */
    List<TransactionReadModel> findByAccountAndDateRange(UUID accountId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Find transactions by account and type
     */
    List<TransactionReadModel> findByAccountAndType(UUID accountId, String transactionType);
    
    /**
     * Find transactions by account and amount range
     */
    List<TransactionReadModel> findByAccountAndAmountRange(UUID accountId, BigDecimal minAmount, BigDecimal maxAmount);
    
    /**
     * Find large transactions above threshold
     */
    List<TransactionReadModel> findLargeTransactions(UUID accountId, BigDecimal threshold);
    
    /**
     * Get transaction statistics for an account
     */
    TransactionStatistics getTransactionStatistics(UUID accountId);
    
    /**
     * Get transaction statistics for a date range
     */
    TransactionStatistics getTransactionStatistics(UUID accountId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Save transaction
     */
    void save(TransactionReadModel transaction);
    
    /**
     * Save multiple transactions
     */
    void saveAll(List<TransactionReadModel> transactions);
    
    /**
     * Delete transaction
     */
    void delete(UUID transactionId);
    
    /**
     * Count transactions for account
     */
    long countByAccount(UUID accountId);
    
    /**
     * Count transactions by account and type
     */
    long countByAccountAndType(UUID accountId, String transactionType);
    
    /**
     * Get last transaction date for account
     */
    Optional<LocalDateTime> getLastTransactionDate(UUID accountId);
    
    /**
     * Transaction statistics data class
     */
    class TransactionStatistics {
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
}