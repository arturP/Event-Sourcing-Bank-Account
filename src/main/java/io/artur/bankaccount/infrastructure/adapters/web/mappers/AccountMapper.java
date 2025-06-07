package io.artur.bankaccount.infrastructure.adapters.web.mappers;

import io.artur.bankaccount.application.commands.models.*;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import io.artur.bankaccount.infrastructure.adapters.web.dto.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccountMapper {
    
    public static OpenAccountCommand toCommand(CreateAccountRequest request) {
        return new OpenAccountCommand(
            UUID.randomUUID(),
            request.getAccountHolder(),
            request.getOverdraftLimit(),
            new EventMetadata(1)
        );
    }
    
    public static DepositMoneyCommand toCommand(UUID accountId, TransactionRequest request) {
        return new DepositMoneyCommand(
            accountId,
            request.getAmount(),
            new EventMetadata(1)
        );
    }
    
    public static WithdrawMoneyCommand toWithdrawCommand(UUID accountId, TransactionRequest request) {
        return new WithdrawMoneyCommand(
            accountId,
            request.getAmount(),
            new EventMetadata(1)
        );
    }
    
    public static TransferMoneyCommand toCommand(UUID fromAccountId, TransferRequest request) {
        return new TransferMoneyCommand(
            fromAccountId,
            request.getToAccountId(),
            request.getAmount(),
            request.getDescription(),
            new EventMetadata(1)
        );
    }
    
    public static AccountResponse toResponse(BankAccount account) {
        return new AccountResponse(
            account.getAccountId(),
            account.getAccountHolder().getFullName(),
            account.getBalance().getAmount(),
            account.getOverdraftLimit().getAmount(),
            LocalDateTime.now(), // Would be from creation event in real implementation
            LocalDateTime.now()
        );
    }
    
    public static TransactionResponse toTransactionResponse(UUID accountId, String type, 
                                                          TransactionRequest request, 
                                                          BankAccount account, 
                                                          boolean successful, String message) {
        return new TransactionResponse(
            accountId,
            type,
            request.getAmount(),
            successful ? account.getBalance().getAmount() : null,
            request.getDescription(),
            LocalDateTime.now(),
            successful,
            message
        );
    }
    
    public static TransactionResponse toTransferResponse(UUID accountId, String type, 
                                                       TransferRequest request, 
                                                       BankAccount account, 
                                                       boolean successful, String message) {
        return new TransactionResponse(
            accountId,
            type,
            request.getAmount(),
            successful ? account.getBalance().getAmount() : null,
            request.getDescription(),
            LocalDateTime.now(),
            successful,
            message
        );
    }
}