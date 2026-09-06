package io.artur.bankaccount.application.ports.incoming;

import io.artur.bankaccount.application.queries.readmodels.AccountStatistics;
import io.artur.bankaccount.application.queries.models.AccountSearchQuery;
import io.artur.bankaccount.application.queries.models.AccountSummaryQuery;
import io.artur.bankaccount.application.queries.readmodels.AccountSummaryReadModel;
import io.artur.bankaccount.application.queries.readmodels.PagedResult;
import java.util.Optional;

public interface AccountSummaryQueryUseCase {
    Optional<AccountSummaryReadModel> getAccountSummary(AccountSummaryQuery query);

    PagedResult<AccountSummaryReadModel> searchAccounts(AccountSearchQuery query);

    AccountStatistics getAccountStatistics();
}
