package io.artur.bankaccount.application.ports.incoming;

import io.artur.bankaccount.application.queries.readmodels.TransactionStatistics;
import io.artur.bankaccount.application.queries.models.TransactionHistoryQuery;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;
import io.artur.bankaccount.application.queries.readmodels.TransactionReadModel;
import java.time.LocalDateTime;
import java.util.UUID;

public interface TransactionQueryUseCase {
    PagedResult<TransactionReadModel> getTransactionHistory(TransactionHistoryQuery query);

    TransactionStatistics getTransactionStatistics(UUID accountId);

    TransactionStatistics getTransactionStatistics(UUID accountId, LocalDateTime from, LocalDateTime to);
}
