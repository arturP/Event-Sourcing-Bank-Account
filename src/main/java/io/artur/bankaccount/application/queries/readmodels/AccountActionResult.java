package io.artur.bankaccount.application.queries.readmodels;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountActionResult(UUID accountId, String action, boolean canPerform,
                                  String reason, BigDecimal currentBalance, String accountStatus) {
}
