package io.artur.bankaccount.api.controller;

import io.artur.bankaccount.api.dto.CreateAccountRequest;
import io.artur.bankaccount.api.dto.AccountResponse;
import io.artur.bankaccount.api.dto.TransactionRequest;
import io.artur.bankaccount.api.dto.TransactionResponse;
import io.artur.bankaccount.api.dto.AccountLifecycleRequest;
import io.artur.bankaccount.api.dto.AccountLifecycleResponse;
import io.artur.bankaccount.api.dto.AccountSummaryResponse;
import io.artur.bankaccount.api.dto.TransactionHistoryResponse;
import io.artur.bankaccount.api.dto.PagedResponse;
import io.artur.bankaccount.api.dto.AccountStatisticsResponse;
import io.artur.bankaccount.api.dto.TransactionStatisticsResponse;
import io.artur.bankaccount.application.commands.models.DepositMoneyCommand;
import io.artur.bankaccount.application.commands.models.OpenAccountCommand;
import io.artur.bankaccount.application.commands.models.WithdrawMoneyCommand;
import io.artur.bankaccount.application.commands.models.TransferMoneyCommand;
import io.artur.bankaccount.application.commands.models.FreezeAccountCommand;
import io.artur.bankaccount.application.commands.models.CloseAccountCommand;
import io.artur.bankaccount.application.commands.models.ReactivateAccountCommand;
import io.artur.bankaccount.application.commands.models.MarkAccountDormantCommand;
import io.artur.bankaccount.application.services.AccountApplicationService;
import io.artur.bankaccount.application.queries.handlers.AccountQueryHandler;
import io.artur.bankaccount.application.queries.handlers.TransactionQueryHandler;
import io.artur.bankaccount.application.queries.models.AccountSearchQuery;
import io.artur.bankaccount.application.queries.models.AccountSummaryQuery;
import io.artur.bankaccount.application.queries.models.TransactionHistoryQuery;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    private final AccountApplicationService applicationService;
    private final AccountQueryHandler accountQueryHandler;
    private final TransactionQueryHandler transactionQueryHandler;
    
    @Autowired
    public AccountController(AccountApplicationService applicationService,
                           AccountQueryHandler accountQueryHandler,
                           TransactionQueryHandler transactionQueryHandler) {
        this.applicationService = applicationService;
        this.accountQueryHandler = accountQueryHandler;
        this.transactionQueryHandler = transactionQueryHandler;
    }
    
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        try {
            EventMetadata metadata = new EventMetadata(1);
            OpenAccountCommand command = new OpenAccountCommand(
                UUID.randomUUID(),
                request.getAccountHolderName(),
                request.getOverdraftLimit(),
                metadata
            );
            
            UUID accountId = applicationService.openAccount(command);
            
            AccountResponse response = new AccountResponse(
                accountId,
                request.getAccountHolderName(),
                request.getOverdraftLimit(),
                request.getOverdraftLimit() // Initial balance is 0, so available balance equals overdraft limit
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId) {
        try {
            Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
            
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            BankAccount account = accountOpt.get();
            BigDecimal availableBalance = account.getBalance().getAmount().add(account.getOverdraftLimit().getAmount());
            AccountResponse response = new AccountResponse(
                accountId,
                account.getAccountHolder().getFullName(),
                account.getBalance().getAmount(),
                availableBalance
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable UUID accountId,
            @RequestBody TransactionRequest request) {
        try {
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            DepositMoneyCommand command = new DepositMoneyCommand(
                accountId,
                request.getAmount(),
                metadata
            );
            
            applicationService.deposit(command);
            
            TransactionResponse response = new TransactionResponse(
                "SUCCESS",
                "Deposit completed successfully",
                request.getAmount()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            TransactionResponse response = new TransactionResponse(
                "FAILED",
                e.getMessage(),
                request.getAmount()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable UUID accountId,
            @RequestBody TransactionRequest request) {
        try {
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            WithdrawMoneyCommand command = new WithdrawMoneyCommand(
                accountId,
                request.getAmount(),
                metadata
            );
            
            applicationService.withdraw(command);
            
            TransactionResponse response = new TransactionResponse(
                "SUCCESS",
                "Withdrawal completed successfully",
                request.getAmount()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            TransactionResponse response = new TransactionResponse(
                "FAILED",
                e.getMessage(),
                request.getAmount()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{fromAccountId}/transfer/{toAccountId}")
    public ResponseEntity<TransactionResponse> transfer(
            @PathVariable UUID fromAccountId,
            @PathVariable UUID toAccountId,
            @RequestBody TransactionRequest request) {
        try {
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            TransferMoneyCommand command = new TransferMoneyCommand(
                fromAccountId,
                toAccountId,
                request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "Transfer",
                metadata
            );
            
            applicationService.transfer(command);
            
            TransactionResponse response = new TransactionResponse(
                "SUCCESS",
                "Transfer completed successfully",
                request.getAmount()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            TransactionResponse response = new TransactionResponse(
                "FAILED",
                e.getMessage(),
                request.getAmount()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    // Account Lifecycle Management Endpoints
    
    @PostMapping("/{accountId}/freeze")
    public ResponseEntity<AccountLifecycleResponse> freezeAccount(
            @PathVariable UUID accountId,
            @RequestBody AccountLifecycleRequest request) {
        try {
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            FreezeAccountCommand command = new FreezeAccountCommand(
                accountId,
                request.getReason(),
                request.getPerformedBy(),
                metadata
            );
            
            applicationService.freezeAccount(command);
            
            AccountLifecycleResponse response = AccountLifecycleResponse.success(
                accountId, "FROZEN", request.getReason(), request.getPerformedBy()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            AccountLifecycleResponse response = AccountLifecycleResponse.failure(accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{accountId}/close")
    public ResponseEntity<AccountLifecycleResponse> closeAccount(
            @PathVariable UUID accountId,
            @RequestBody AccountLifecycleRequest request) {
        try {
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            CloseAccountCommand command = new CloseAccountCommand(
                accountId,
                request.getReason(),
                request.getPerformedBy(),
                metadata
            );
            
            applicationService.closeAccount(command);
            
            AccountLifecycleResponse response = AccountLifecycleResponse.success(
                accountId, "CLOSED", request.getReason(), request.getPerformedBy()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            AccountLifecycleResponse response = AccountLifecycleResponse.failure(accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{accountId}/reactivate")
    public ResponseEntity<AccountLifecycleResponse> reactivateAccount(
            @PathVariable UUID accountId,
            @RequestBody AccountLifecycleRequest request) {
        try {
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            ReactivateAccountCommand command = new ReactivateAccountCommand(
                accountId,
                request.getReason(),
                request.getPerformedBy(),
                metadata
            );
            
            applicationService.reactivateAccount(command);
            
            AccountLifecycleResponse response = AccountLifecycleResponse.success(
                accountId, "ACTIVE", request.getReason(), request.getPerformedBy()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            AccountLifecycleResponse response = AccountLifecycleResponse.failure(accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{accountId}/mark-dormant")
    public ResponseEntity<AccountLifecycleResponse> markAccountDormant(
            @PathVariable UUID accountId,
            @RequestBody AccountLifecycleRequest request) {
        try {
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            MarkAccountDormantCommand command = new MarkAccountDormantCommand(
                accountId,
                request.getReason(),
                request.getPerformedBy(),
                metadata
            );
            
            applicationService.markAccountDormant(command);
            
            AccountLifecycleResponse response = AccountLifecycleResponse.success(
                accountId, "DORMANT", request.getReason(), request.getPerformedBy()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            AccountLifecycleResponse response = AccountLifecycleResponse.failure(accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    // Query Endpoints
    
    @GetMapping("/{accountId}/summary")
    public ResponseEntity<AccountSummaryResponse> getAccountSummary(@PathVariable UUID accountId) {
        try {
            AccountSummaryQuery query = new AccountSummaryQuery(accountId);
            return accountQueryHandler.getAccountSummary(query)
                .map(readModel -> ResponseEntity.ok(AccountSummaryResponse.fromReadModel(readModel)))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<AccountSummaryResponse>> searchAccounts(
            @RequestParam(defaultValue = "") String holderName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) BigDecimal maxBalance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "accountHolderName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        try {
            AccountSearchQuery query = new AccountSearchQuery(
                holderName.isEmpty() ? null : holderName,
                status,
                minBalance,
                maxBalance,
                page,
                size,
                sortBy,
                sortDirection
            );
            
            PagedResponse<AccountSummaryResponse> response = PagedResponse.fromPagedResult(
                accountQueryHandler.searchAccounts(query),
                AccountSummaryResponse::fromReadModel
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<AccountStatisticsResponse> getAccountStatistics() {
        try {
            AccountStatisticsResponse response = AccountStatisticsResponse.fromStatistics(
                accountQueryHandler.getAccountStatistics()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<PagedResponse<TransactionHistoryResponse>> getTransactionHistory(
            @PathVariable UUID accountId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            TransactionHistoryQuery query = new TransactionHistoryQuery(
                accountId,
                fromDate != null ? java.time.LocalDateTime.parse(fromDate + "T00:00:00") : null,
                toDate != null ? java.time.LocalDateTime.parse(toDate + "T23:59:59") : null,
                transactionType,
                minAmount,
                maxAmount,
                page,
                size
            );
            
            PagedResponse<TransactionHistoryResponse> response = PagedResponse.fromPagedResult(
                transactionQueryHandler.getTransactionHistory(query),
                TransactionHistoryResponse::fromReadModel
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{accountId}/transactions/statistics")
    public ResponseEntity<TransactionStatisticsResponse> getTransactionStatistics(
            @PathVariable UUID accountId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            TransactionStatisticsResponse response;
            
            if (fromDate != null && toDate != null) {
                response = TransactionStatisticsResponse.fromStatistics(
                    transactionQueryHandler.getTransactionStatistics(
                        accountId,
                        java.time.LocalDateTime.parse(fromDate + "T00:00:00"),
                        java.time.LocalDateTime.parse(toDate + "T23:59:59")
                    )
                );
            } else {
                response = TransactionStatisticsResponse.fromStatistics(
                    transactionQueryHandler.getTransactionStatistics(accountId)
                );
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}