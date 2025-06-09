package io.artur.bankaccount.application.ports.outgoing;

import io.artur.bankaccount.application.queries.models.AccountSearchQuery;
import io.artur.bankaccount.application.queries.readmodels.AccountSummaryReadModel;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;

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
    
    /**
     * Account statistics data class
     */
    class AccountStatistics {
        private final long totalAccounts;
        private final long activeAccounts;
        private final long frozenAccounts;
        private final long closedAccounts;
        private final long dormantAccounts;
        private final java.math.BigDecimal totalBalance;
        private final java.math.BigDecimal averageBalance;
        
        public AccountStatistics(long totalAccounts, long activeAccounts, long frozenAccounts,
                               long closedAccounts, long dormantAccounts, java.math.BigDecimal totalBalance,
                               java.math.BigDecimal averageBalance) {
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
        public java.math.BigDecimal getTotalBalance() { return totalBalance; }
        public java.math.BigDecimal getAverageBalance() { return averageBalance; }
    }
}