package io.artur.bankaccount.application.queries.projections;

import io.artur.bankaccount.application.ports.outgoing.AccountSummaryQueryRepository;
import io.artur.bankaccount.application.queries.readmodels.AccountSummaryReadModel;
import io.artur.bankaccount.domain.account.events.AccountClosedEvent;
import io.artur.bankaccount.domain.account.events.AccountFrozenEvent;
import io.artur.bankaccount.domain.account.events.AccountOpenedEvent;
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
public class AccountSummaryProjectionHandler {
    
    private final AccountSummaryQueryRepository repository;
    private final Executor projectionExecutor;
    
    public AccountSummaryProjectionHandler(AccountSummaryQueryRepository repository) {
        this.repository = repository;
        this.projectionExecutor = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "projection-handler");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void handle(AccountOpenedEvent event) {
        AccountSummaryReadModel readModel = AccountSummaryReadModel.fromAccountOpened(
            event.getId(),
            event.getId().toString(),
            event.getAccountHolder(),
            event.getOverdraftLimit()
        );
        
        repository.save(readModel);
    }
    
    public CompletableFuture<Void> handleAsync(AccountOpenedEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    public void handle(MoneyDepositedEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                BigDecimal currentBalance = readModel.getBalance();
                BigDecimal newBalance = currentBalance.add(event.getAmount());
                readModel.updateBalance(newBalance);
                repository.save(readModel);
            });
    }
    
    public CompletableFuture<Void> handleAsync(MoneyDepositedEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    public void handle(MoneyWithdrawnEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                BigDecimal currentBalance = readModel.getBalance();
                BigDecimal newBalance = currentBalance.subtract(event.getAmount());
                readModel.updateBalance(newBalance);
                repository.save(readModel);
            });
    }
    
    public CompletableFuture<Void> handleAsync(MoneyWithdrawnEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    public void handle(MoneyTransferredEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                BigDecimal currentBalance = readModel.getBalance();
                BigDecimal newBalance = currentBalance.subtract(event.getAmount());
                readModel.updateBalance(newBalance);
                repository.save(readModel);
            });
    }
    
    public CompletableFuture<Void> handleAsync(MoneyTransferredEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    public void handle(MoneyReceivedEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                BigDecimal currentBalance = readModel.getBalance();
                BigDecimal newBalance = currentBalance.add(event.getAmount());
                readModel.updateBalance(newBalance);
                repository.save(readModel);
            });
    }
    
    public CompletableFuture<Void> handleAsync(MoneyReceivedEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    public void handle(AccountFrozenEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                readModel.updateStatus("FROZEN", event.getFrozenBy(), event.getReason());
                repository.save(readModel);
            });
    }
    
    public CompletableFuture<Void> handleAsync(AccountFrozenEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
    
    public void handle(AccountClosedEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                readModel.updateStatus("CLOSED", event.getClosedBy(), "Account closed");
                readModel.updateBalance(event.getFinalBalance().getAmount());
                repository.save(readModel);
            });
    }
    
    public CompletableFuture<Void> handleAsync(AccountClosedEvent event) {
        return CompletableFuture.runAsync(() -> handle(event), projectionExecutor);
    }
}