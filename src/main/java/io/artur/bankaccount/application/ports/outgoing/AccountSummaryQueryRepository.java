package io.artur.bankaccount.application.ports.outgoing;

import io.artur.bankaccount.application.queries.models.AccountSearchQuery;
import io.artur.bankaccount.application.queries.readmodels.AccountSummaryReadModel;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;

import io.artur.bankaccount.application.queries.readmodels.AccountStatistics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query repository for account summary read models
 * Provides optimized read operations for account data
 */
public interface AccountSummaryQueryRepository {
    
    /**
     * Find account summary by account ID
     */
    Optional<AccountSummaryReadModel> findByAccountId(UUID accountId);
    
    /**
     * Find all account summaries
     */
    List<AccountSummaryReadModel> findAll();
    
    /**
     * Search accounts with pagination and filtering
     */
    PagedResult<AccountSummaryReadModel> search(AccountSearchQuery query);
    
    /**
     * Find accounts by holder name (partial match)
     */
    List<AccountSummaryReadModel> findByAccountHolderNameContaining(String holderName);
    
    /**
     * Find accounts by status
     */
    List<AccountSummaryReadModel> findByAccountStatus(String status);
    
    /**
     * Find accounts with balance above threshold
     */
    List<AccountSummaryReadModel> findByBalanceGreaterThan(java.math.BigDecimal threshold);
    
    /**
     * Find accounts with balance below threshold
     */
    List<AccountSummaryReadModel> findByBalanceLessThan(java.math.BigDecimal threshold);
    
    /**
     * Find dormant accounts (no recent transactions)
     */
    List<AccountSummaryReadModel> findDormantAccounts(int daysWithoutActivity);
    
    /**
     * Get account statistics
     */
    AccountStatistics getAccountStatistics();
    
    /**
     * Save or update account summary
     */
    void save(AccountSummaryReadModel accountSummary);
    
    /**
     * Delete account summary
     */
    void delete(UUID accountId);
    
    /**
     * Check if account summary exists
     */
    boolean exists(UUID accountId);
    
    /**
     * Count total accounts
     */
    long count();
    
    /**
     * Count accounts by status
     */
    long countByStatus(String status);
    
}
