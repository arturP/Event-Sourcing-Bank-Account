package io.artur.bankaccount.application.ports.incoming;

import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.valueobjects.Money;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountQueryUseCase {
    
    Optional<BankAccount> findAccountById(UUID accountId);
    
    List<BankAccount> findAllAccounts();
    
    Money getAccountBalance(UUID accountId);
}