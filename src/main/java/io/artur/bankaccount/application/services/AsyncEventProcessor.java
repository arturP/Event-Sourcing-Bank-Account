package io.artur.bankaccount.application.services;

import io.artur.bankaccount.application.queries.projections.AccountSummaryProjectionHandler;
import io.artur.bankaccount.application.queries.projections.TransactionProjectionHandler;
import io.artur.bankaccount.domain.account.events.*;
import io.artur.bankaccount.domain.shared.events.DomainEvent;
import io.artur.bankaccount.domain.account.events.AccountDomainEvent;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class AsyncEventProcessor {
    
    private final AccountSummaryProjectionHandler accountSummaryHandler;
    private final TransactionProjectionHandler transactionHandler;
    private final Executor eventProcessingExecutor;
    
    public AsyncEventProcessor(AccountSummaryProjectionHandler accountSummaryHandler,
                              TransactionProjectionHandler transactionHandler) {
        this.accountSummaryHandler = accountSummaryHandler;
        this.transactionHandler = transactionHandler;
        this.eventProcessingExecutor = Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "async-event-processor");
            t.setDaemon(true);
            return t;
        });
    }
    
    public CompletableFuture<Void> processEventAsync(DomainEvent event) {
        return CompletableFuture.runAsync(() -> {
            processEventProjections(event);
        }, eventProcessingExecutor);
    }
    
    private void processEventProjections(DomainEvent event) {
        if (event instanceof AccountOpenedEvent e) {
            accountSummaryHandler.handleAsync(e);
        } else if (event instanceof MoneyDepositedEvent e) {
            CompletableFuture.allOf(
                accountSummaryHandler.handleAsync(e),
                transactionHandler.handleAsync(e)
            ).join();
        } else if (event instanceof MoneyWithdrawnEvent e) {
            CompletableFuture.allOf(
                accountSummaryHandler.handleAsync(e),
                transactionHandler.handleAsync(e)
            ).join();
        } else if (event instanceof MoneyTransferredEvent e) {
            CompletableFuture.allOf(
                accountSummaryHandler.handleAsync(e),
                transactionHandler.handleAsync(e)
            ).join();
        } else if (event instanceof MoneyReceivedEvent e) {
            CompletableFuture.allOf(
                accountSummaryHandler.handleAsync(e),
                transactionHandler.handleAsync(e)
            ).join();
        } else if (event instanceof AccountFrozenEvent e) {
            accountSummaryHandler.handleAsync(e);
        } else if (event instanceof AccountClosedEvent e) {
            accountSummaryHandler.handleAsync(e);
        } else {
            // Log unhandled event types
            System.out.println("Unhandled event type: " + event.getClass().getSimpleName());
        }
    }
    
    public CompletableFuture<Void> processEventsAsync(java.util.List<DomainEvent> events) {
        CompletableFuture<Void>[] futures = events.stream()
                .map(this::processEventAsync)
                .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(futures);
    }
    
    public CompletableFuture<Void> processAccountEventsAsync(java.util.List<AccountDomainEvent> events) {
        CompletableFuture<Void>[] futures = events.stream()
                .map(event -> processEventAsync((DomainEvent) event))
                .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(futures);
    }
}