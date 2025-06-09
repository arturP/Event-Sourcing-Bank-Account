package io.artur.bankaccount.infrastructure.persistence.queries;

import io.artur.bankaccount.application.ports.outgoing.TransactionHistoryQueryRepository;
import io.artur.bankaccount.application.queries.models.TransactionHistoryQuery;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;
import io.artur.bankaccount.application.queries.readmodels.TransactionReadModel;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class NativeTransactionHistoryQueryRepository implements TransactionHistoryQueryRepository {
    
    private final Map<UUID, TransactionReadModel> storage = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> accountTransactions = new ConcurrentHashMap<>();
    
    @Override
    public Optional<TransactionReadModel> findByTransactionId(UUID transactionId) {
        return Optional.ofNullable(storage.get(transactionId));
    }
    
    @Override
    public PagedResult<TransactionReadModel> getTransactionHistory(TransactionHistoryQuery query) {
        List<TransactionReadModel> accountTxns = getTransactionsByAccount(query.getAccountId());
        
        // Apply filters
        List<TransactionReadModel> filtered = accountTxns.stream()
            .filter(txn -> matchesFilter(txn, query))
            .sorted(Comparator.comparing(TransactionReadModel::getTimestamp).reversed())
            .collect(Collectors.toList());
        
        // Apply pagination
        int start = query.getPage() * query.getSize();
        int end = Math.min(start + query.getSize(), filtered.size());
        
        List<TransactionReadModel> pageContent = start < filtered.size() 
            ? filtered.subList(start, end) 
            : Collections.emptyList();
        
        return PagedResult.of(pageContent, query.getPage(), query.getSize(), filtered.size());
    }
    
    @Override
    public List<TransactionReadModel> getRecentTransactions(UUID accountId, int limit) {
        return getTransactionsByAccount(accountId).stream()
            .sorted(Comparator.comparing(TransactionReadModel::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TransactionReadModel> findByAccountAndDateRange(UUID accountId, LocalDateTime from, LocalDateTime to) {
        return getTransactionsByAccount(accountId).stream()
            .filter(txn -> txn.getTimestamp() != null && 
                          !txn.getTimestamp().isBefore(from) && 
                          !txn.getTimestamp().isAfter(to))
            .sorted(Comparator.comparing(TransactionReadModel::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TransactionReadModel> findByAccountAndType(UUID accountId, String transactionType) {
        return getTransactionsByAccount(accountId).stream()
            .filter(txn -> transactionType.equals(txn.getTransactionType()))
            .sorted(Comparator.comparing(TransactionReadModel::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TransactionReadModel> findByAccountAndAmountRange(UUID accountId, BigDecimal minAmount, BigDecimal maxAmount) {
        return getTransactionsByAccount(accountId).stream()
            .filter(txn -> txn.getAmount() != null && 
                          txn.getAmount().compareTo(minAmount) >= 0 && 
                          txn.getAmount().compareTo(maxAmount) <= 0)
            .sorted(Comparator.comparing(TransactionReadModel::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TransactionReadModel> findLargeTransactions(UUID accountId, BigDecimal threshold) {
        return getTransactionsByAccount(accountId).stream()
            .filter(txn -> txn.getAmount() != null && txn.getAmount().compareTo(threshold) > 0)
            .sorted(Comparator.comparing(TransactionReadModel::getAmount).reversed())
            .collect(Collectors.toList());
    }
    
    @Override
    public TransactionStatistics getTransactionStatistics(UUID accountId) {
        List<TransactionReadModel> transactions = getTransactionsByAccount(accountId);
        return calculateStatistics(transactions);
    }
    
    @Override
    public TransactionStatistics getTransactionStatistics(UUID accountId, LocalDateTime from, LocalDateTime to) {
        List<TransactionReadModel> transactions = findByAccountAndDateRange(accountId, from, to);
        return calculateStatistics(transactions);
    }
    
    @Override
    public void save(TransactionReadModel transaction) {
        storage.put(transaction.getTransactionId(), transaction);
        
        // Update account transaction index
        accountTransactions.computeIfAbsent(transaction.getAccountId(), k -> new ArrayList<>())
            .add(transaction.getTransactionId());
    }
    
    @Override
    public void saveAll(List<TransactionReadModel> transactions) {
        transactions.forEach(this::save);
    }
    
    @Override
    public void delete(UUID transactionId) {
        TransactionReadModel transaction = storage.remove(transactionId);
        if (transaction != null) {
            List<UUID> accountTxns = accountTransactions.get(transaction.getAccountId());
            if (accountTxns != null) {
                accountTxns.remove(transactionId);
            }
        }
    }
    
    @Override
    public long countByAccount(UUID accountId) {
        List<UUID> transactions = accountTransactions.get(accountId);
        return transactions != null ? transactions.size() : 0;
    }
    
    @Override
    public long countByAccountAndType(UUID accountId, String transactionType) {
        return getTransactionsByAccount(accountId).stream()
            .mapToLong(txn -> transactionType.equals(txn.getTransactionType()) ? 1 : 0)
            .sum();
    }
    
    @Override
    public Optional<LocalDateTime> getLastTransactionDate(UUID accountId) {
        return getTransactionsByAccount(accountId).stream()
            .map(TransactionReadModel::getTimestamp)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo);
    }
    
    private List<TransactionReadModel> getTransactionsByAccount(UUID accountId) {
        List<UUID> transactionIds = accountTransactions.get(accountId);
        if (transactionIds == null) {
            return Collections.emptyList();
        }
        
        return transactionIds.stream()
            .map(storage::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private boolean matchesFilter(TransactionReadModel transaction, TransactionHistoryQuery query) {
        if (query.getFromDate() != null && 
            (transaction.getTimestamp() == null || transaction.getTimestamp().isBefore(query.getFromDate()))) {
            return false;
        }
        
        if (query.getToDate() != null && 
            (transaction.getTimestamp() == null || transaction.getTimestamp().isAfter(query.getToDate()))) {
            return false;
        }
        
        if (query.getTransactionType() != null && 
            !query.getTransactionType().equals(transaction.getTransactionType())) {
            return false;
        }
        
        if (query.getMinAmount() != null && 
            (transaction.getAmount() == null || transaction.getAmount().compareTo(query.getMinAmount()) < 0)) {
            return false;
        }
        
        if (query.getMaxAmount() != null && 
            (transaction.getAmount() == null || transaction.getAmount().compareTo(query.getMaxAmount()) > 0)) {
            return false;
        }
        
        return true;
    }
    
    private TransactionStatistics calculateStatistics(List<TransactionReadModel> transactions) {
        if (transactions.isEmpty()) {
            return new TransactionStatistics(0, 0, 0, 0, 0, 
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        }
        
        long totalTransactions = transactions.size();
        long depositCount = transactions.stream().mapToLong(t -> "DEPOSIT".equals(t.getTransactionType()) ? 1 : 0).sum();
        long withdrawalCount = transactions.stream().mapToLong(t -> "WITHDRAWAL".equals(t.getTransactionType()) ? 1 : 0).sum();
        long transferInCount = transactions.stream().mapToLong(t -> "TRANSFER_IN".equals(t.getTransactionType()) ? 1 : 0).sum();
        long transferOutCount = transactions.stream().mapToLong(t -> "TRANSFER_OUT".equals(t.getTransactionType()) ? 1 : 0).sum();
        
        BigDecimal totalDeposits = transactions.stream()
            .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
            .map(TransactionReadModel::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalWithdrawals = transactions.stream()
            .filter(t -> "WITHDRAWAL".equals(t.getTransactionType()))
            .map(TransactionReadModel::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalTransferIn = transactions.stream()
            .filter(t -> "TRANSFER_IN".equals(t.getTransactionType()))
            .map(TransactionReadModel::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalTransferOut = transactions.stream()
            .filter(t -> "TRANSFER_OUT".equals(t.getTransactionType()))
            .map(TransactionReadModel::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAmount = transactions.stream()
            .map(TransactionReadModel::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageTransactionAmount = totalTransactions > 0 
            ? totalAmount.divide(BigDecimal.valueOf(totalTransactions), 2, BigDecimal.ROUND_HALF_UP)
            : BigDecimal.ZERO;
        
        BigDecimal largestTransaction = transactions.stream()
            .map(TransactionReadModel::getAmount)
            .filter(Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal smallestTransaction = transactions.stream()
            .map(TransactionReadModel::getAmount)
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        LocalDateTime firstTransactionDate = transactions.stream()
            .map(TransactionReadModel::getTimestamp)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        
        LocalDateTime lastTransactionDate = transactions.stream()
            .map(TransactionReadModel::getTimestamp)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        return new TransactionStatistics(totalTransactions, depositCount, withdrawalCount, 
            transferInCount, transferOutCount, totalDeposits, totalWithdrawals, 
            totalTransferIn, totalTransferOut, averageTransactionAmount, 
            largestTransaction, smallestTransaction, firstTransactionDate, lastTransactionDate);
    }
}