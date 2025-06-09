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

@Component
public class AccountSummaryProjectionHandler {
    
    private final AccountSummaryQueryRepository repository;
    
    public AccountSummaryProjectionHandler(AccountSummaryQueryRepository repository) {
        this.repository = repository;
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
    
    public void handle(MoneyDepositedEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                BigDecimal currentBalance = readModel.getBalance();
                BigDecimal newBalance = currentBalance.add(event.getAmount());
                readModel.updateBalance(newBalance);
                repository.save(readModel);
            });
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
    
    public void handle(MoneyTransferredEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                BigDecimal currentBalance = readModel.getBalance();
                BigDecimal newBalance = currentBalance.subtract(event.getAmount());
                readModel.updateBalance(newBalance);
                repository.save(readModel);
            });
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
    
    public void handle(AccountFrozenEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                readModel.updateStatus("FROZEN", event.getFrozenBy(), event.getReason());
                repository.save(readModel);
            });
    }
    
    public void handle(AccountClosedEvent event) {
        repository.findByAccountId(event.getId())
            .ifPresent(readModel -> {
                readModel.updateStatus("CLOSED", event.getClosedBy(), "Account closed");
                readModel.updateBalance(event.getFinalBalance().getAmount());
                repository.save(readModel);
            });
    }
}