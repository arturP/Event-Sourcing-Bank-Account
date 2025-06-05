package io.artur.eventsourcing.projections;

import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.events.AccountOpenedEvent;
import io.artur.eventsourcing.events.MoneyDepositedEvent;
import io.artur.eventsourcing.events.MoneyWithdrawnEvent;

public class AccountSummaryProjectionHandler implements ProjectionHandler<AccountSummaryProjection> {
    
    private AccountSummaryProjection projection;
    
    public AccountSummaryProjectionHandler() {
        this.projection = new AccountSummaryProjection();
    }
    
    @Override
    public void handle(AccountEvent event) {
        if (event instanceof AccountOpenedEvent openEvent) {
            handleAccountOpened(openEvent);
        } else if (event instanceof MoneyDepositedEvent depositEvent) {
            handleMoneyDeposited(depositEvent);
        } else if (event instanceof MoneyWithdrawnEvent withdrawEvent) {
            handleMoneyWithdrawn(withdrawEvent);
        }
    }
    
    private void handleAccountOpened(AccountOpenedEvent event) {
        projection.setAccountId(event.getId());
        projection.setAccountHolder(event.getAccountHolder());
        projection.setOverdraftLimit(event.getOverdraftLimit());
        projection.setAccountOpenedDate(event.getTimestamp());
        projection.setLastTransactionDate(event.getTimestamp());
    }
    
    private void handleMoneyDeposited(MoneyDepositedEvent event) {
        projection.addToBalance(event.getAmount());
        projection.incrementTransactionCount();
        projection.setLastTransactionDate(event.getTimestamp());
    }
    
    private void handleMoneyWithdrawn(MoneyWithdrawnEvent event) {
        projection.subtractFromBalance(event.getAmount());
        projection.incrementTransactionCount();
        projection.setLastTransactionDate(event.getTimestamp());
    }
    
    @Override
    public AccountSummaryProjection getProjection() {
        return projection;
    }
    
    @Override
    public void reset() {
        this.projection = new AccountSummaryProjection();
    }
}