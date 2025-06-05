package io.artur.eventsourcing.cqrs.handlers;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.commands.OpenAccountCommand;
import io.artur.eventsourcing.cqrs.CommandHandler;
import io.artur.eventsourcing.domain.AccountHolder;
import io.artur.eventsourcing.domain.AccountNumber;
import io.artur.eventsourcing.domain.Money;
import io.artur.eventsourcing.domain.services.AccountValidationService;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.repository.BankAccountRepository;

import java.util.UUID;

public class OpenAccountCommandHandler implements CommandHandler<OpenAccountCommand> {
    
    private final EventStore<AccountEvent, UUID> eventStore;
    private final BankAccountRepository repository;
    private final AccountValidationService validationService;
    
    public OpenAccountCommandHandler(EventStore<AccountEvent, UUID> eventStore, 
                                   BankAccountRepository repository,
                                   AccountValidationService validationService) {
        this.eventStore = eventStore;
        this.repository = repository;
        this.validationService = validationService;
    }
    
    @Override
    public void handle(OpenAccountCommand command) {
        command.validate();
        
        // Create value objects
        AccountNumber accountNumber = AccountNumber.generate();
        AccountHolder accountHolder = AccountHolder.of(command.getAccountHolder());
        Money overdraftLimit = Money.of(command.getOverdraftLimit());
        
        // Domain validation
        validationService.validateAccountCreation(accountNumber, accountHolder, overdraftLimit)
                .throwIfInvalid();
        
        // Create and save aggregate
        BankAccount account = new BankAccount(eventStore);
        account.openAccount(command.getAccountHolder(), command.getOverdraftLimit(), command.getMetadata());
        
        repository.save(account);
    }
}