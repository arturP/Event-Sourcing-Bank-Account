package io.artur.bankaccount.application.services;

import io.artur.bankaccount.application.commands.models.*;
import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceTest {

    @Mock
    private AccountRepository accountRepository;
    
    private AccountApplicationService applicationService;
    private EventMetadata metadata;
    
    @BeforeEach
    void setUp() {
        applicationService = new AccountApplicationService(accountRepository);
        metadata = new EventMetadata(1);
    }
    
    @Test
    void shouldOpenNewAccount() {
        UUID accountId = UUID.randomUUID();
        OpenAccountCommand command = new OpenAccountCommand(
            accountId, 
            "John Doe", 
            BigDecimal.valueOf(500), 
            metadata
        );
        
        UUID result = applicationService.openAccount(command);
        
        assertNotNull(result);
        verify(accountRepository).save(any(BankAccount.class));
    }
    
    @Test
    void shouldDepositMoney() {
        UUID accountId = UUID.randomUUID();
        BankAccount account = BankAccount.openNewAccount("John Doe", BigDecimal.valueOf(100), metadata);
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        
        DepositMoneyCommand command = new DepositMoneyCommand(accountId, BigDecimal.valueOf(200), metadata);
        
        assertDoesNotThrow(() -> applicationService.deposit(command));
        
        verify(accountRepository).findById(accountId);
        verify(accountRepository).save(account);
    }
    
    @Test
    void shouldWithdrawMoney() {
        UUID accountId = UUID.randomUUID();
        BankAccount account = BankAccount.openNewAccount("John Doe", BigDecimal.valueOf(100), metadata);
        account.deposit(BigDecimal.valueOf(300), metadata);
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        
        WithdrawMoneyCommand command = new WithdrawMoneyCommand(accountId, BigDecimal.valueOf(150), metadata);
        
        assertDoesNotThrow(() -> applicationService.withdraw(command));
        
        verify(accountRepository).findById(accountId);
        verify(accountRepository).save(account);
    }
    
    @Test
    void shouldTransferMoney() {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        
        BankAccount fromAccount = BankAccount.openNewAccount("Alice Smith", BigDecimal.valueOf(100), metadata);
        fromAccount.deposit(BigDecimal.valueOf(500), metadata);
        
        BankAccount toAccount = BankAccount.openNewAccount("Bob Jones", BigDecimal.ZERO, metadata);
        toAccount.deposit(BigDecimal.valueOf(100), metadata);
        
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
        
        TransferMoneyCommand command = new TransferMoneyCommand(
            fromAccountId, 
            toAccountId, 
            BigDecimal.valueOf(200), 
            "Test transfer", 
            metadata
        );
        
        assertDoesNotThrow(() -> applicationService.transfer(command));
        
        verify(accountRepository).findById(fromAccountId);
        verify(accountRepository).findById(toAccountId);
        verify(accountRepository, times(2)).save(any(BankAccount.class));
    }
    
    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        UUID accountId = UUID.randomUUID();
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
        
        DepositMoneyCommand command = new DepositMoneyCommand(accountId, BigDecimal.valueOf(100), metadata);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> applicationService.deposit(command)
        );
        
        assertTrue(exception.getMessage().contains("Account not found"));
    }
}