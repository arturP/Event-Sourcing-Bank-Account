package io.artur.bankaccount.application.ports.outgoing;

import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.account.valueobjects.AccountNumber;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    
    void save(BankAccount account);
    
    Optional<BankAccount> findById(UUID accountId);
    
    Optional<BankAccount> findByAccountNumber(AccountNumber accountNumber);
    
    List<BankAccount> findAll();
    
    List<BankAccount> findByAccountHolder(String accountHolderName);
    
    boolean exists(UUID accountId);
    
    void delete(UUID accountId);
}