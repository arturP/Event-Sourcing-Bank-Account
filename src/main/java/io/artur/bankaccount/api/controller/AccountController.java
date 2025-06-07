package io.artur.bankaccount.api.controller;

import io.artur.bankaccount.api.dto.CreateAccountRequest;
import io.artur.bankaccount.api.dto.AccountResponse;
import io.artur.bankaccount.api.dto.TransactionRequest;
import io.artur.bankaccount.api.dto.TransactionResponse;
import io.artur.bankaccount.application.commands.models.DepositMoneyCommand;
import io.artur.bankaccount.application.commands.models.OpenAccountCommand;
import io.artur.bankaccount.application.commands.models.WithdrawMoneyCommand;
import io.artur.bankaccount.application.commands.models.TransferMoneyCommand;
import io.artur.bankaccount.application.services.AccountApplicationService;
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
    
    @Autowired
    public AccountController(AccountApplicationService applicationService) {
        this.applicationService = applicationService;
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
}