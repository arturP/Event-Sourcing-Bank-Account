package io.artur.eventsourcing.cqrs.handlers;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.commands.TransferMoneyCommand;
import io.artur.eventsourcing.cqrs.CommandHandler;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.repository.BankAccountRepository;

import java.util.UUID;

public class TransferMoneyCommandHandler implements CommandHandler<TransferMoneyCommand> {
    
    private final EventStore<AccountEvent, UUID> eventStore;
    private final BankAccountRepository repository;
    
    public TransferMoneyCommandHandler(EventStore<AccountEvent, UUID> eventStore, 
                                     BankAccountRepository repository) {
        this.eventStore = eventStore;
        this.repository = repository;
    }
    
    @Override
    public void handle(TransferMoneyCommand command) {
        command.validate();
        
        // Load both accounts
        BankAccount fromAccount = repository.findById(command.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("From account not found: " + command.getFromAccountId()));
        
        BankAccount toAccount = repository.findById(command.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException("To account not found: " + command.getToAccountId()));
        
        // Perform the transfer
        fromAccount.transferOut(command.getToAccountId(), command.getAmount(), command.getDescription(), command.getMetadata());
        toAccount.receiveTransfer(command.getFromAccountId(), command.getAmount(), command.getDescription(), command.getMetadata());
        
        // Save both accounts
        repository.save(fromAccount);
        repository.save(toAccount);
    }
}