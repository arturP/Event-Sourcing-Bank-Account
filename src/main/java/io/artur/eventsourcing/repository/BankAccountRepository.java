package io.artur.eventsourcing.repository;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.domain.AccountNumber;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends Repository<BankAccount, UUID> {
    Optional<BankAccount> findByAccountNumber(AccountNumber accountNumber);
    List<BankAccount> findByAccountHolder(String accountHolderName);
    List<BankAccount> findAccountsWithOverdraft();
    List<BankAccount> findAccountsWithNegativeBalance();
}