package io.artur.bankaccount.application.queries.projections;

import io.artur.bankaccount.application.ports.outgoing.TransactionHistoryQueryRepository;
import io.artur.bankaccount.application.queries.readmodels.TransactionReadModel;
import io.artur.bankaccount.domain.account.events.MoneyDepositedEvent;
import io.artur.bankaccount.domain.account.events.MoneyReceivedEvent;
import io.artur.bankaccount.domain.account.events.MoneyTransferredEvent;
import io.artur.bankaccount.domain.account.events.MoneyWithdrawnEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class TransactionProjectionHandler {
    
    private final TransactionHistoryQueryRepository repository;
    private final Executor projectionExecutor;
    
    public TransactionProjectionHandler(TransactionHistoryQueryRepository repository) {
        this.repository = repository;
        this.projectionExecutor = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "transaction-projection");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void handle(MoneyDepositedEvent event) {
        TransactionReadModel transaction = TransactionReadModel.createDeposit(
            event.getId(),
            event.getAmount(),
            String.format("Money deposited: %s", event.getAmount()),
            convertToLocalDateTime(event.getTimestamp()),
            BigDecimal.ZERO // Will be updated by projection
        );
        
        repository.save(transaction);
    }
    
    public CompletableFuture<Void> handleAsync(MoneyDepositedEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    public void handle(MoneyWithdrawnEvent event) {
        TransactionReadModel transaction = TransactionReadModel.createWithdrawal(
            event.getId(),
            event.getAmount(),
            String.format("Money withdrawn: %s", event.getAmount()),
            convertToLocalDateTime(event.getTimestamp()),
            BigDecimal.ZERO // Will be updated by projection
        );
        
        repository.save(transaction);
    }
    
    public CompletableFuture<Void> handleAsync(MoneyWithdrawnEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    public void handle(MoneyTransferredEvent event) {
        TransactionReadModel transaction = TransactionReadModel.createTransferOut(
            event.getId(),
            event.getToAccountId(),
            event.getAmount(),
            String.format("Money transferred to %s: %s", event.getToAccountId(), event.getAmount()),
            convertToLocalDateTime(event.getTimestamp()),
            BigDecimal.ZERO // Will be updated by projection
        );
        
        repository.save(transaction);
    }
    
    public CompletableFuture<Void> handleAsync(MoneyTransferredEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    public void handle(MoneyReceivedEvent event) {
        TransactionReadModel transaction = TransactionReadModel.createTransferIn(
            event.getId(),
            event.getFromAccountId(),
            event.getAmount(),
            String.format("Money received from %s: %s", event.getFromAccountId(), event.getAmount()),
            convertToLocalDateTime(event.getTimestamp()),
            BigDecimal.ZERO // Will be updated by projection
        );
        
        repository.save(transaction);
    }
    
    public CompletableFuture<Void> handleAsync(MoneyReceivedEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    private LocalDateTime convertToLocalDateTime(java.time.LocalDateTime timestamp) {
        return timestamp;
    }
}