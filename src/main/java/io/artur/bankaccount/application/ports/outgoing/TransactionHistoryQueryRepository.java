package io.artur.bankaccount.application.ports.outgoing;

import io.artur.bankaccount.application.queries.models.TransactionHistoryQuery;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;
import io.artur.bankaccount.application.queries.readmodels.TransactionReadModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.artur.bankaccount.application.queries.readmodels.TransactionStatistics;

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
    
}
