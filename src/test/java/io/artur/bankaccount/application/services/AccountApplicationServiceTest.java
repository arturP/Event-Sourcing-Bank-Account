package io.artur.bankaccount.application.services;

import io.artur.bankaccount.application.commands.models.*;
import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.application.ports.outgoing.CachePort;
import io.artur.bankaccount.application.queries.readmodels.AccountDetailsResult;
import io.artur.bankaccount.domain.shared.valueobjects.Money;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.util.Map;
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
            new CommandContext(1)
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
        
        DepositMoneyCommand command = new DepositMoneyCommand(accountId, BigDecimal.valueOf(200), new CommandContext(1));
        
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
        
        WithdrawMoneyCommand command = new WithdrawMoneyCommand(accountId, BigDecimal.valueOf(150), new CommandContext(1));
        
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
            new CommandContext(1)
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
        
        DepositMoneyCommand command = new DepositMoneyCommand(accountId, BigDecimal.valueOf(100), new CommandContext(1));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> applicationService.deposit(command)
        );
        
        assertTrue(exception.getMessage().contains("Account not found"));
    }

    @Test
    void shouldReturnDetachedAccountDetailsIncludingAvailableBalanceAndStatus() {
        UUID id = UUID.randomUUID();
        BankAccount account = BankAccount.openNewAccount(id, "Jane Smith", new BigDecimal("100.00"), metadata);
        account.withdraw(new BigDecimal("25.00"), metadata);
        account.freeze("Review", "operator", metadata);
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        when(accountRepository.findAll()).thenReturn(List.of(account));

        AccountDetailsResult details = applicationService.findAccountById(id).orElseThrow();

        assertEquals(new AccountDetailsResult(id, "Jane Smith", new BigDecimal("-25.00"),
                new BigDecimal("100.00"), new BigDecimal("75.00"), "FROZEN"), details);
        assertEquals(List.of(details), applicationService.findAllAccounts());
        account.reactivate("Reviewed", "operator", metadata);
        assertEquals("FROZEN", details.status());
        assertTrue(applicationService.findAccountById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void shouldReturnBalanceAsApplicationDataForRepositoryAndCacheReads() {
        UUID id = UUID.randomUUID();
        BankAccount account = BankAccount.openNewAccount(id, "Jane Smith", BigDecimal.ZERO, metadata);
        account.deposit(new BigDecimal("25.00"), metadata);
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        assertEquals(new BigDecimal("25.00"), applicationService.getAccountBalance(id));

        CachePort cache = mock(CachePort.class);
        when(cache.getCachedBalance(id)).thenReturn(Optional.of(Money.of("42.00")));
        AccountApplicationService cachedService = new AccountApplicationService(accountRepository, cache, null);
        assertEquals(new BigDecimal("42.00"), cachedService.getAccountBalance(id));
        verify(accountRepository, times(1)).findById(id);
    }

    @Test
    void shouldCreateDomainMetadataFromContextAndShareItAcrossTransferEvents() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        BankAccount from = BankAccount.openNewAccount(fromId, "Alice Smith", new BigDecimal("100.00"), metadata);
        BankAccount to = BankAccount.openNewAccount(toId, "Bob Jones", BigDecimal.ZERO, metadata);
        from.markEventsAsCommitted();
        to.markEventsAsCommitted();
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));
        CommandContext context = new CommandContext("correlation", "causation", "operator",
                "client", "127.0.0.1", 3, Map.of("source", "test"));

        applicationService.transfer(new TransferMoneyCommand(fromId, toId, new BigDecimal("25.00"), "Transfer", context));

        EventMetadata eventMetadata = from.getUncommittedEvents().getFirst().getMetadata();
        assertSame(eventMetadata, to.getUncommittedEvents().getFirst().getMetadata());
        assertEquals(context.correlationId(), eventMetadata.getCorrelationId());
        assertEquals(context.causationId(), eventMetadata.getCausationId());
        assertEquals(context.userId(), eventMetadata.getUserId());
        assertEquals(context.userAgent(), eventMetadata.getUserAgent());
        assertEquals(context.ipAddress(), eventMetadata.getIpAddress());
        assertEquals(context.version(), eventMetadata.getVersion());
        assertEquals(context.additionalProperties(), eventMetadata.getAdditionalProperties());
        assertNotNull(eventMetadata.getTimestamp());
    }
}
