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
import io.artur.bankaccount.api.dto.UpdateOverdraftLimitRequest;
import io.artur.bankaccount.api.dto.BulkAccountOperationRequest;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@Validated
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
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
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
            @Valid @RequestBody TransactionRequest request) {
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
            @Valid @RequestBody TransactionRequest request) {
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
            @Valid @RequestBody TransactionRequest request) {
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
            @Valid @RequestBody AccountLifecycleRequest request) {
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
            @Valid @RequestBody AccountLifecycleRequest request) {
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
            @Valid @RequestBody AccountLifecycleRequest request) {
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
            @Valid @RequestBody AccountLifecycleRequest request) {
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
    
    // Enhanced Account Management Operations
    
    @PostMapping("/{accountId}/suspend")
    public ResponseEntity<AccountLifecycleResponse> suspendAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountLifecycleRequest request) {
        try {
            // Check account exists first
            Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // For now, suspend is implemented as freeze with different status
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            FreezeAccountCommand command = new FreezeAccountCommand(
                accountId,
                "SUSPENDED: " + request.getReason(),
                request.getPerformedBy(),
                metadata
            );
            
            applicationService.freezeAccount(command);
            
            AccountLifecycleResponse response = AccountLifecycleResponse.success(
                accountId, "SUSPENDED", request.getReason(), request.getPerformedBy()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            AccountLifecycleResponse response = AccountLifecycleResponse.failure(accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{accountId}/lock")
    public ResponseEntity<AccountLifecycleResponse> lockAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountLifecycleRequest request) {
        try {
            // Check account exists first
            Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // For now, lock is implemented as freeze with different status
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            FreezeAccountCommand command = new FreezeAccountCommand(
                accountId,
                "LOCKED: " + request.getReason(),
                request.getPerformedBy(),
                metadata
            );
            
            applicationService.freezeAccount(command);
            
            AccountLifecycleResponse response = AccountLifecycleResponse.success(
                accountId, "LOCKED", request.getReason(), request.getPerformedBy()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            AccountLifecycleResponse response = AccountLifecycleResponse.failure(accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/{accountId}/unlock")
    public ResponseEntity<AccountLifecycleResponse> unlockAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountLifecycleRequest request) {
        try {
            // Check account exists first
            Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
            ReactivateAccountCommand command = new ReactivateAccountCommand(
                accountId,
                "UNLOCKED: " + request.getReason(),
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
    
    @GetMapping("/{accountId}/lifecycle-history")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getAccountLifecycleHistory(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be non-negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size cannot exceed 100") int size) {
        try {
            // Check account exists first
            Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // For now, return mock lifecycle history - this would need proper event sourcing implementation
            java.util.List<Map<String, Object>> lifecycleEvents = java.util.List.of(
                Map.of(
                    "timestamp", java.time.LocalDateTime.now().minusDays(30).toString(),
                    "action", "ACCOUNT_OPENED",
                    "performedBy", "SYSTEM",
                    "reason", "Initial account creation",
                    "previousStatus", "NONE",
                    "newStatus", "ACTIVE"
                ),
                Map.of(
                    "timestamp", java.time.LocalDateTime.now().minusDays(15).toString(),
                    "action", "ACCOUNT_FROZEN",
                    "performedBy", "admin@bank.com",
                    "reason", "Suspicious activity detected",
                    "previousStatus", "ACTIVE",
                    "newStatus", "FROZEN"
                ),
                Map.of(
                    "timestamp", java.time.LocalDateTime.now().minusDays(10).toString(),
                    "action", "ACCOUNT_REACTIVATED",
                    "performedBy", "admin@bank.com",
                    "reason", "Investigation completed - no issues found",
                    "previousStatus", "FROZEN",
                    "newStatus", "ACTIVE"
                )
            );
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, lifecycleEvents.size());
            java.util.List<Map<String, Object>> pagedEvents = lifecycleEvents.subList(start, end);
            
            PagedResponse<Map<String, Object>> response = new PagedResponse<>(
                pagedEvents,
                page,
                size,
                lifecycleEvents.size(),
                (int) Math.ceil((double) lifecycleEvents.size() / size),
                page < (int) Math.ceil((double) lifecycleEvents.size() / size) - 1, // hasNext
                page > 0, // hasPrevious
                page == 0, // first
                page == (int) Math.ceil((double) lifecycleEvents.size() / size) - 1, // last
                pagedEvents.isEmpty(), // empty
                pagedEvents.size() // numberOfElements
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{accountId}/can-perform/{action}")
    public ResponseEntity<Map<String, Object>> canPerformAction(
            @PathVariable UUID accountId,
            @PathVariable @Pattern(regexp = "DEPOSIT|WITHDRAW|TRANSFER|FREEZE|CLOSE|REACTIVATE", message = "Invalid action type") String action) {
        try {
            Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            BankAccount account = accountOpt.get();
            
            // Business logic to determine if action can be performed
            boolean canPerform = true;
            String reason = "Action is allowed";
            
            // TODO: Implement proper status checking from BankAccount
            // For now, use simple logic based on current state
            switch (action.toUpperCase()) {
                case "DEPOSIT":
                case "WITHDRAW":
                case "TRANSFER":
                    // These require account to be active
                    canPerform = true; // TODO: Check account status
                    break;
                case "FREEZE":
                    canPerform = true; // TODO: Check if already frozen
                    break;
                case "CLOSE":
                    // Can close if balance is zero or positive
                    canPerform = account.getBalance().getAmount().compareTo(BigDecimal.ZERO) >= 0;
                    if (!canPerform) {
                        reason = "Cannot close account with negative balance";
                    }
                    break;
                case "REACTIVATE":
                    canPerform = true; // TODO: Check if account is frozen/suspended
                    break;
                default:
                    canPerform = false;
                    reason = "Unknown action";
            }
            
            Map<String, Object> response = Map.of(
                "accountId", accountId,
                "action", action,
                "canPerform", canPerform,
                "reason", reason,
                "currentBalance", account.getBalance().getAmount(),
                "accountStatus", "ACTIVE" // TODO: Get actual status
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{accountId}/restrictions")
    public ResponseEntity<Map<String, Object>> getAccountRestrictions(@PathVariable UUID accountId) {
        try {
            Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            BankAccount account = accountOpt.get();
            
            // TODO: Implement proper restriction checking from domain model
            Map<String, Object> restrictions = Map.of(
                "accountId", accountId,
                "canDeposit", true,
                "canWithdraw", true,
                "canTransfer", true,
                "dailyWithdrawalLimit", 10000.00,
                "monthlyTransferLimit", 50000.00,
                "overdraftLimit", account.getOverdraftLimit().getAmount(),
                "status", "ACTIVE", // TODO: Get actual status
                "restrictionReasons", java.util.List.of(),
                "lastUpdated", java.time.LocalDateTime.now().toString()
            );
            
            return ResponseEntity.ok(restrictions);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Bulk Account Management Operations
    
    @PostMapping("/bulk-operation")
    public ResponseEntity<Map<String, Object>> performBulkAccountOperation(
            @Valid @RequestBody BulkAccountOperationRequest request) {
        try {
            java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
            int successCount = 0;
            int failureCount = 0;
            
            for (UUID accountId : request.getAccountIds()) {
                try {
                    // Check if account exists
                    Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
                    if (accountOpt.isEmpty()) {
                        results.add(Map.of(
                            "accountId", accountId,
                            "status", "FAILED",
                            "reason", "Account not found"
                        ));
                        failureCount++;
                        continue;
                    }
                    
                    // Perform the operation based on type
                    EventMetadata metadata = new EventMetadata((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
                    AccountLifecycleRequest lifecycleRequest = new AccountLifecycleRequest(request.getReason(), request.getPerformedBy());
                    
                    switch (request.getOperationType().toUpperCase()) {
                        case "FREEZE":
                            FreezeAccountCommand freezeCommand = new FreezeAccountCommand(
                                accountId, request.getReason(), request.getPerformedBy(), metadata);
                            applicationService.freezeAccount(freezeCommand);
                            break;
                            
                        case "UNFREEZE":
                        case "REACTIVATE":
                            ReactivateAccountCommand reactivateCommand = new ReactivateAccountCommand(
                                accountId, request.getReason(), request.getPerformedBy(), metadata);
                            applicationService.reactivateAccount(reactivateCommand);
                            break;
                            
                        case "SUSPEND":
                            FreezeAccountCommand suspendCommand = new FreezeAccountCommand(
                                accountId, "SUSPENDED: " + request.getReason(), request.getPerformedBy(), metadata);
                            applicationService.freezeAccount(suspendCommand);
                            break;
                            
                        case "CLOSE":
                            CloseAccountCommand closeCommand = new CloseAccountCommand(
                                accountId, request.getReason(), request.getPerformedBy(), metadata);
                            applicationService.closeAccount(closeCommand);
                            break;
                            
                        case "MARK_DORMANT":
                            MarkAccountDormantCommand dormantCommand = new MarkAccountDormantCommand(
                                accountId, request.getReason(), request.getPerformedBy(), metadata);
                            applicationService.markAccountDormant(dormantCommand);
                            break;
                            
                        default:
                            results.add(Map.of(
                                "accountId", accountId,
                                "status", "FAILED",
                                "reason", "Unknown operation type: " + request.getOperationType()
                            ));
                            failureCount++;
                            continue;
                    }
                    
                    results.add(Map.of(
                        "accountId", accountId,
                        "status", "SUCCESS",
                        "operation", request.getOperationType(),
                        "reason", request.getReason()
                    ));
                    successCount++;
                    
                } catch (Exception e) {
                    results.add(Map.of(
                        "accountId", accountId,
                        "status", "FAILED",
                        "reason", e.getMessage()
                    ));
                    failureCount++;
                    
                    // In STRICT mode, stop on first failure
                    if ("STRICT".equals(request.getExecutionMode())) {
                        break;
                    }
                }
            }
            
            Map<String, Object> response = Map.of(
                "operationType", request.getOperationType(),
                "executionMode", request.getExecutionMode(),
                "totalAccounts", request.getAccountIds().size(),
                "successCount", successCount,
                "failureCount", failureCount,
                "performedBy", request.getPerformedBy(),
                "timestamp", java.time.LocalDateTime.now().toString(),
                "results", results
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "status", "FAILED",
                "message", "Bulk operation failed: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    // Additional Utility Endpoints
    
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getAccountBalance(@PathVariable UUID accountId) {
        try {
            Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
            
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            BankAccount account = accountOpt.get();
            BigDecimal availableBalance = account.getBalance().getAmount().add(account.getOverdraftLimit().getAmount());
            
            Map<String, Object> balanceInfo = Map.of(
                "accountId", accountId,
                "currentBalance", account.getBalance().getAmount(),
                "overdraftLimit", account.getOverdraftLimit().getAmount(),
                "availableBalance", availableBalance,
                "status", "ACTIVE" // TODO: Implement getStatus() method in BankAccount
            );
            
            return ResponseEntity.ok(balanceInfo);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{accountId}/status")
    public ResponseEntity<Map<String, Object>> getAccountStatus(@PathVariable UUID accountId) {
        try {
            Optional<BankAccount> accountOpt = applicationService.findAccountById(accountId);
            
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            BankAccount account = accountOpt.get();
            
            Map<String, Object> statusInfo = Map.of(
                "accountId", accountId,
                "status", "ACTIVE", // TODO: Implement getStatus() method in BankAccount
                "accountHolder", account.getAccountHolder().getFullName(),
                "isActive", true, // TODO: Implement proper status checking
                "canTransact", true // TODO: Implement proper status checking
            );
            
            return ResponseEntity.ok(statusInfo);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{accountId}/overdraft-limit")
    public ResponseEntity<AccountResponse> updateOverdraftLimit(
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateOverdraftLimitRequest request) {
        try {
            // This would need to be implemented in the application service
            // For now, return method not allowed
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
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
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in format YYYY-MM-DD") String fromDate,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in format YYYY-MM-DD") String toDate,
            @RequestParam(required = false) @Pattern(regexp = "DEPOSIT|WITHDRAWAL|TRANSFER_IN|TRANSFER_OUT", message = "Invalid transaction type") String transactionType,
            @RequestParam(required = false) @DecimalMin(value = "0.0", message = "Minimum amount must be non-negative") BigDecimal minAmount,
            @RequestParam(required = false) @DecimalMin(value = "0.0", message = "Maximum amount must be non-negative") BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be non-negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size cannot exceed 100") int size) {
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
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in format YYYY-MM-DD") String fromDate,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in format YYYY-MM-DD") String toDate) {
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
    
    // Additional Transaction History Endpoints
    
    @GetMapping("/{accountId}/transactions/recent")
    public ResponseEntity<PagedResponse<TransactionHistoryResponse>> getRecentTransactions(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 50, message = "Limit cannot exceed 50") int limit) {
        try {
            TransactionHistoryQuery query = new TransactionHistoryQuery(
                accountId,
                null, // No date filters for recent transactions
                null,
                null, // No transaction type filter
                null, // No amount filters
                null,
                0, // First page
                limit
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
    
    @GetMapping("/{accountId}/transactions/by-type/{transactionType}")
    public ResponseEntity<PagedResponse<TransactionHistoryResponse>> getTransactionsByType(
            @PathVariable UUID accountId,
            @PathVariable @Pattern(regexp = "DEPOSIT|WITHDRAWAL|TRANSFER_IN|TRANSFER_OUT", message = "Invalid transaction type") String transactionType,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be non-negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size cannot exceed 100") int size) {
        try {
            TransactionHistoryQuery query = new TransactionHistoryQuery(
                accountId,
                null, // No date filters
                null,
                transactionType,
                null, // No amount filters
                null,
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
    
    @GetMapping("/{accountId}/transactions/today")
    public ResponseEntity<PagedResponse<TransactionHistoryResponse>> getTodaysTransactions(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be non-negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size cannot exceed 100") int size) {
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDateTime startOfDay = today.atStartOfDay();
            java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);
            
            TransactionHistoryQuery query = new TransactionHistoryQuery(
                accountId,
                startOfDay,
                endOfDay,
                null, // No transaction type filter
                null, // No amount filters
                null,
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
    
    @GetMapping("/{accountId}/transactions/monthly")
    public ResponseEntity<TransactionStatisticsResponse> getMonthlyTransactionStatistics(
            @PathVariable UUID accountId,
            @RequestParam(required = false) @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must be in format YYYY-MM") String month) {
        try {
            java.time.LocalDateTime fromDate;
            java.time.LocalDateTime toDate;
            
            if (month != null) {
                java.time.YearMonth yearMonth = java.time.YearMonth.parse(month);
                fromDate = yearMonth.atDay(1).atStartOfDay();
                toDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            } else {
                java.time.YearMonth currentMonth = java.time.YearMonth.now();
                fromDate = currentMonth.atDay(1).atStartOfDay();
                toDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);
            }
            
            TransactionStatisticsResponse response = TransactionStatisticsResponse.fromStatistics(
                transactionQueryHandler.getTransactionStatistics(accountId, fromDate, toDate)
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}