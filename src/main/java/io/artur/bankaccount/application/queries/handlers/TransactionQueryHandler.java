package io.artur.bankaccount.application.queries.handlers;

import io.artur.bankaccount.application.ports.outgoing.TransactionHistoryQueryRepository;
import io.artur.bankaccount.application.queries.models.TransactionHistoryQuery;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;
import io.artur.bankaccount.application.queries.readmodels.TransactionReadModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionQueryHandler {
    
    private final TransactionHistoryQueryRepository repository;
    
    public TransactionQueryHandler(TransactionHistoryQueryRepository repository) {
        this.repository = repository;
    }
    
    public Optional<TransactionReadModel> getTransaction(UUID transactionId) {
        return repository.findByTransactionId(transactionId);
    }
    
    public PagedResult<TransactionReadModel> getTransactionHistory(TransactionHistoryQuery query) {
        return repository.getTransactionHistory(query);
    }
    
    public List<TransactionReadModel> getRecentTransactions(UUID accountId, int limit) {
        return repository.getRecentTransactions(accountId, limit);
    }
    
    public List<TransactionReadModel> getTransactionsByDateRange(UUID accountId, LocalDateTime from, LocalDateTime to) {
        return repository.findByAccountAndDateRange(accountId, from, to);
    }
    
    public List<TransactionReadModel> getTransactionsByType(UUID accountId, String transactionType) {
        return repository.findByAccountAndType(accountId, transactionType);
    }
    
    public List<TransactionReadModel> getTransactionsByAmountRange(UUID accountId, BigDecimal minAmount, BigDecimal maxAmount) {
        return repository.findByAccountAndAmountRange(accountId, minAmount, maxAmount);
    }
    
    public List<TransactionReadModel> getLargeTransactions(UUID accountId, BigDecimal threshold) {
        return repository.findLargeTransactions(accountId, threshold);
    }
    
    public TransactionHistoryQueryRepository.TransactionStatistics getTransactionStatistics(UUID accountId) {
        return repository.getTransactionStatistics(accountId);
    }
    
    public TransactionHistoryQueryRepository.TransactionStatistics getTransactionStatistics(UUID accountId, 
                                                                                           LocalDateTime from, 
                                                                                           LocalDateTime to) {
        return repository.getTransactionStatistics(accountId, from, to);
    }
    
    public long getTransactionCount(UUID accountId) {
        return repository.countByAccount(accountId);
    }
    
    public long getTransactionCountByType(UUID accountId, String transactionType) {
        return repository.countByAccountAndType(accountId, transactionType);
    }
    
    public Optional<LocalDateTime> getLastTransactionDate(UUID accountId) {
        return repository.getLastTransactionDate(accountId);
    }
}