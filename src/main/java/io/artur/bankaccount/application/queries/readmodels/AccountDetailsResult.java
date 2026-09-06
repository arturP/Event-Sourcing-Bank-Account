package io.artur.bankaccount.application.queries.readmodels;

import java.math.BigDecimal;
import java.util.UUID;

/** Detached account data exposed by the application, without access to the aggregate. */
public record AccountDetailsResult(UUID accountId,
                                   String accountHolderName,
                                   BigDecimal balance,
                                   BigDecimal overdraftLimit,
                                   BigDecimal availableBalance,
                                   String status) {
}
