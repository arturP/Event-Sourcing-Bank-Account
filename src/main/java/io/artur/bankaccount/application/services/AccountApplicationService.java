package io.artur.bankaccount.application.services;

import io.artur.bankaccount.application.commands.models.*;
import io.artur.bankaccount.application.ports.incoming.AccountManagementUseCase;
import io.artur.bankaccount.application.ports.incoming.AccountQueryUseCase;
import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.valueobjects.Money;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AccountApplicationService implements AccountManagementUseCase, AccountQueryUseCase {
    
    private final AccountRepository accountRepository;
    
    public AccountApplicationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    @Override
    public UUID openAccount(OpenAccountCommand command) {
        command.validate();
        
        BankAccount account = BankAccount.openNewAccount(
            command.getAccountId(),
            command.getAccountHolder(),
            command.getOverdraftLimit(),
            command.getMetadata()
        );
        
        accountRepository.save(account);
        return account.getAccountId();
    }
    
    @Override
    public void deposit(DepositMoneyCommand command) {
        command.validate();
        
        BankAccount account = accountRepository.findById(command.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + command.getAccountId()));
        
        account.deposit(command.getAmount(), command.getMetadata());
        accountRepository.save(account);
    }
    
    @Override
    public void withdraw(WithdrawMoneyCommand command) {
        command.validate();
        
        BankAccount account = accountRepository.findById(command.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + command.getAccountId()));
        
        account.withdraw(command.getAmount(), command.getMetadata());
        accountRepository.save(account);
    }
    
    @Override
    public void transfer(TransferMoneyCommand command) {
        command.validate();
        
        BankAccount fromAccount = accountRepository.findById(command.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("From account not found: " + command.getFromAccountId()));
        
        BankAccount toAccount = accountRepository.findById(command.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException("To account not found: " + command.getToAccountId()));
        
        fromAccount.transferOut(command.getToAccountId(), command.getAmount(), command.getDescription(), command.getMetadata());
        toAccount.receiveTransfer(command.getFromAccountId(), command.getAmount(), command.getDescription(), command.getMetadata());
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }
    
    @Override
    public Optional<BankAccount> findAccountById(UUID accountId) {
        return accountRepository.findById(accountId);
    }
    
    @Override
    public List<BankAccount> findAllAccounts() {
        return accountRepository.findAll();
    }
    
    @Override
    public Money getAccountBalance(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(BankAccount::getBalance)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }
}