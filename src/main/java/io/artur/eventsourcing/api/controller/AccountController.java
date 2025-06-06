package io.artur.eventsourcing.api.controller;

import io.artur.eventsourcing.aggregates.BankAccount;
import io.artur.eventsourcing.api.dto.*;
import io.artur.eventsourcing.eventstores.EventStore;
import io.artur.eventsourcing.events.AccountEvent;
import io.artur.eventsourcing.metrics.PerformanceMetricsCollector;
import io.artur.eventsourcing.repository.EventSourcingBankAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Bank Accounts", description = "Bank account management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountController {
    
    private final EventSourcingBankAccountRepository repository;
    private final EventStore<AccountEvent, UUID> eventStore;
    private final PerformanceMetricsCollector metricsCollector;
    
    public AccountController(EventSourcingBankAccountRepository repository,
                           EventStore<AccountEvent, UUID> eventStore,
                           PerformanceMetricsCollector metricsCollector) {
        this.repository = repository;
        this.eventStore = eventStore;
        this.metricsCollector = metricsCollector;
    }
    
    @PostMapping
    @Operation(summary = "Create new account", description = "Create a new bank account")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return metricsCollector.recordCommandProcessing(() -> {
            BankAccount account = new BankAccount(eventStore);
            account.openAccount(request.getAccountHolder(), request.getOverdraftLimit());
            
            if (request.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
                account.deposit(request.getInitialBalance());
            }
            
            repository.save(account);
            metricsCollector.recordAccountCreation();
            
            AccountResponse response = new AccountResponse(
                account.getAccountId(),
                account.getAccountHolder(),
                account.getBalance(),
                account.getOverdraftLimit(),
                LocalDateTime.now(),
                LocalDateTime.now()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        });
    }
    
    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID", description = "Retrieve account details by account ID")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "Account ID") @PathVariable UUID accountId) {
        
        Optional<BankAccount> accountOpt = repository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        BankAccount account = accountOpt.get();
        AccountResponse response = new AccountResponse(
            account.getAccountId(),
            account.getAccountHolder(),
            account.getBalance(),
            account.getOverdraftLimit(),
            LocalDateTime.now(), // Would be from creation event in real implementation
            LocalDateTime.now()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all accounts", description = "Retrieve all bank accounts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<BankAccount> accounts = repository.findAll();
        
        List<AccountResponse> responses = accounts.stream()
            .map(account -> new AccountResponse(
                account.getAccountId(),
                account.getAccountHolder(),
                account.getBalance(),
                account.getOverdraftLimit(),
                LocalDateTime.now(),
                LocalDateTime.now()
            ))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/{accountId}/deposit")
    @Operation(summary = "Deposit money", description = "Deposit money to the specified account")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> deposit(
            @Parameter(description = "Account ID") @PathVariable UUID accountId,
            @Valid @RequestBody TransactionRequest request) {
        
        return processTransaction(accountId, request, "DEPOSIT", 
            (account, amount) -> {
                account.deposit(amount);
                metricsCollector.recordDeposit();
            });
    }
    
    @PostMapping("/{accountId}/withdraw")
    @Operation(summary = "Withdraw money", description = "Withdraw money from the specified account")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> withdraw(
            @Parameter(description = "Account ID") @PathVariable UUID accountId,
            @Valid @RequestBody TransactionRequest request) {
        
        return processTransaction(accountId, request, "WITHDRAWAL",
            (account, amount) -> {
                account.withdraw(amount);
                metricsCollector.recordWithdrawal();
            });
    }
    
    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance", description = "Get current balance of the specified account")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getBalance(@Parameter(description = "Account ID") @PathVariable UUID accountId) {
        // Try cached balance first for performance
        Optional<BigDecimal> cachedBalance = repository.getCachedBalance(accountId);
        if (cachedBalance.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "balance", cachedBalance.get(),
                "cached", true
            ));
        }
        
        Optional<BankAccount> accountOpt = repository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "accountId", accountId,
            "balance", accountOpt.get().getBalance(),
            "cached", false
        ));
    }
    
    private ResponseEntity<TransactionResponse> processTransaction(
            UUID accountId, 
            TransactionRequest request, 
            String type,
            TransactionProcessor processor) {
        
        return metricsCollector.recordCommandProcessing(() -> {
            try {
                Optional<BankAccount> accountOpt = repository.findById(accountId);
                if (accountOpt.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                
                BankAccount account = accountOpt.get();
                BigDecimal balanceBefore = account.getBalance();
                
                processor.process(account, request.getAmount());
                repository.save(account);
                
                TransactionResponse response = new TransactionResponse(
                    accountId,
                    type,
                    request.getAmount(),
                    account.getBalance(),
                    request.getDescription(),
                    LocalDateTime.now(),
                    true,
                    type + " successful"
                );
                
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                if (type.equals("WITHDRAWAL")) {
                    metricsCollector.recordOverdraftAttempt();
                }
                
                TransactionResponse response = new TransactionResponse(
                    accountId,
                    type,
                    request.getAmount(),
                    null,
                    request.getDescription(),
                    LocalDateTime.now(),
                    false,
                    e.getMessage()
                );
                
                return ResponseEntity.badRequest().body(response);
            }
        });
    }
    
    @FunctionalInterface
    private interface TransactionProcessor {
        void process(BankAccount account, BigDecimal amount);
    }
    
    // Helper class for Map.of compatibility
    private static class Map {
        public static <K, V> java.util.Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
            java.util.Map<K, V> map = new java.util.HashMap<>();
            map.put(k1, v1);
            map.put(k2, v2);
            map.put(k3, v3);
            return map;
        }
    }
}