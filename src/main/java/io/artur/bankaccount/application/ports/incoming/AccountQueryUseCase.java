package io.artur.bankaccount.application.ports.incoming;

import io.artur.bankaccount.application.queries.readmodels.AccountDetailsResult;
import io.artur.bankaccount.application.queries.readmodels.AccountActionResult;

import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountQueryUseCase {
    
    Optional<AccountDetailsResult> findAccountById(UUID accountId);
    
    List<AccountDetailsResult> findAllAccounts();
    
    BigDecimal getAccountBalance(UUID accountId);

    Optional<AccountActionResult> canPerformAction(UUID accountId, String action);
}