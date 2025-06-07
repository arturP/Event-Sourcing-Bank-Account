package io.artur.bankaccount.infrastructure.adapters.web.controllers;

import io.artur.bankaccount.application.commands.models.*;
import io.artur.bankaccount.application.ports.incoming.AccountManagementUseCase;
import io.artur.bankaccount.application.ports.incoming.AccountQueryUseCase;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import io.artur.bankaccount.infrastructure.adapters.web.dto.*;
import io.artur.bankaccount.infrastructure.adapters.web.mappers.AccountMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/accounts")  // v2 to distinguish from legacy API
@Tag(name = "Bank Accounts V2", description = "New domain-driven bank account management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountController {
    
    private final AccountManagementUseCase accountManagementUseCase;
    private final AccountQueryUseCase accountQueryUseCase;
    
    public AccountController(AccountManagementUseCase accountManagementUseCase,
                           AccountQueryUseCase accountQueryUseCase) {
        this.accountManagementUseCase = accountManagementUseCase;
        this.accountQueryUseCase = accountQueryUseCase;
    }
    
    @PostMapping
    @Operation(summary = "Create new account", description = "Create a new bank account using new domain model")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        try {
            OpenAccountCommand command = AccountMapper.toCommand(request);
            UUID accountId = accountManagementUseCase.openAccount(command);
            
            // If initial balance provided, deposit it
            if (request.getInitialBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
                DepositMoneyCommand depositCommand = new DepositMoneyCommand(
                    accountId, 
                    request.getInitialBalance(), 
                    new EventMetadata(1)
                );
                accountManagementUseCase.deposit(depositCommand);
            }
            
            // Retrieve the created account
            Optional<BankAccount> accountOpt = accountQueryUseCase.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            AccountResponse response = AccountMapper.toResponse(accountOpt.get());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID", description = "Retrieve account details by account ID using new domain model")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "Account ID") @PathVariable UUID accountId) {
        
        Optional<BankAccount> accountOpt = accountQueryUseCase.findAccountById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        AccountResponse response = AccountMapper.toResponse(accountOpt.get());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all accounts", description = "Retrieve all bank accounts using new domain model")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<BankAccount> accounts = accountQueryUseCase.findAllAccounts();
        
        List<AccountResponse> responses = accounts.stream()
            .map(AccountMapper::toResponse)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/{accountId}/deposit")
    @Operation(summary = "Deposit money", description = "Deposit money to the specified account using new domain model")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> deposit(
            @Parameter(description = "Account ID") @PathVariable UUID accountId,
            @Valid @RequestBody TransactionRequest request) {
        
        try {
            DepositMoneyCommand command = AccountMapper.toCommand(accountId, request);
            accountManagementUseCase.deposit(command);
            
            // Get updated account for response
            Optional<BankAccount> accountOpt = accountQueryUseCase.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TransactionResponse response = AccountMapper.toTransactionResponse(
                accountId, "DEPOSIT", request, accountOpt.get(), true, "Deposit successful"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            TransactionResponse response = AccountMapper.toTransactionResponse(
                accountId, "DEPOSIT", request, null, false, e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/{accountId}/withdraw")
    @Operation(summary = "Withdraw money", description = "Withdraw money from the specified account using new domain model")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> withdraw(
            @Parameter(description = "Account ID") @PathVariable UUID accountId,
            @Valid @RequestBody TransactionRequest request) {
        
        try {
            WithdrawMoneyCommand command = AccountMapper.toWithdrawCommand(accountId, request);
            accountManagementUseCase.withdraw(command);
            
            // Get updated account for response
            Optional<BankAccount> accountOpt = accountQueryUseCase.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TransactionResponse response = AccountMapper.toTransactionResponse(
                accountId, "WITHDRAWAL", request, accountOpt.get(), true, "Withdrawal successful"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            TransactionResponse response = AccountMapper.toTransactionResponse(
                accountId, "WITHDRAWAL", request, null, false, e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/{accountId}/transfer")
    @Operation(summary = "Transfer money", description = "Transfer money from this account to another account using new domain model")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> transfer(
            @Parameter(description = "From Account ID") @PathVariable UUID accountId,
            @Valid @RequestBody TransferRequest request) {
        
        try {
            TransferMoneyCommand command = AccountMapper.toCommand(accountId, request);
            accountManagementUseCase.transfer(command);
            
            // Get updated account for response
            Optional<BankAccount> accountOpt = accountQueryUseCase.findAccountById(accountId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TransactionResponse response = AccountMapper.toTransferResponse(
                accountId, "TRANSFER", request, accountOpt.get(), true, 
                "Transfer successful to account " + request.getToAccountId()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            TransactionResponse response = AccountMapper.toTransferResponse(
                accountId, "TRANSFER", request, null, false, e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance", description = "Get current balance of the specified account using new domain model")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getBalance(@Parameter(description = "Account ID") @PathVariable UUID accountId) {
        try {
            io.artur.bankaccount.domain.shared.valueobjects.Money balance = accountQueryUseCase.getAccountBalance(accountId);
            
            return ResponseEntity.ok(java.util.Map.of(
                "accountId", accountId,
                "balance", balance.getAmount(),
                "currency", balance.getCurrency().getCurrencyCode(),
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}