package io.artur.bankaccount.application.services;

import io.artur.bankaccount.application.queries.projections.AccountSummaryProjectionHandler;
import io.artur.bankaccount.application.queries.projections.TransactionProjectionHandler;
import io.artur.bankaccount.domain.account.events.*;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncEventProcessorTest {

    @Mock
    private AccountSummaryProjectionHandler accountSummaryHandler;
    
    @Mock
    private TransactionProjectionHandler transactionHandler;
    
    private AsyncEventProcessor eventProcessor;
    private EventMetadata metadata;
    
    @BeforeEach
    void setUp() {
        eventProcessor = new AsyncEventProcessor(accountSummaryHandler, transactionHandler);
        metadata = new EventMetadata(1);
        
        // Use lenient() to avoid unnecessary stubbing warnings
        lenient().when(accountSummaryHandler.handleAsync(any(AccountOpenedEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(accountSummaryHandler.handleAsync(any(MoneyDepositedEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(transactionHandler.handleAsync(any(MoneyDepositedEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(transactionHandler.handleAsync(any(MoneyWithdrawnEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(transactionHandler.handleAsync(any(MoneyTransferredEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(transactionHandler.handleAsync(any(MoneyReceivedEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
    }
    
    @Test
    void shouldProcessAccountOpenedEventAsync() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountOpenedEvent event = new AccountOpenedEvent(
            accountId, 
            "John Doe", 
            BigDecimal.valueOf(500), 
            metadata
        );
        
        CompletableFuture<Void> future = eventProcessor.processEventAsync(event);
        future.get(5, TimeUnit.SECONDS);
        
        verify(accountSummaryHandler).handleAsync(event);
        verify(transactionHandler, never()).handleAsync(any(MoneyDepositedEvent.class));
    }
    
    @Test
    void shouldProcessMoneyDepositedEventAsync() throws Exception {
        UUID accountId = UUID.randomUUID();
        MoneyDepositedEvent event = new MoneyDepositedEvent(
            accountId, 
            BigDecimal.valueOf(100), 
            metadata
        );
        
        CompletableFuture<Void> future = eventProcessor.processEventAsync(event);
        future.get(5, TimeUnit.SECONDS);
        
        verify(accountSummaryHandler).handleAsync(event);
        verify(transactionHandler).handleAsync((MoneyDepositedEvent) event);
    }
    
    @Test
    void shouldProcessMultipleEventsAsync() throws Exception {
        UUID accountId = UUID.randomUUID();
        List<AccountDomainEvent> events = List.of(
            new AccountOpenedEvent(accountId, "John Doe", BigDecimal.valueOf(500), metadata),
            new MoneyDepositedEvent(accountId, BigDecimal.valueOf(100), metadata)
        );
        
        CompletableFuture<Void> future = eventProcessor.processAccountEventsAsync(events);
        future.get(5, TimeUnit.SECONDS);
        
        verify(accountSummaryHandler, times(1)).handleAsync(any(AccountOpenedEvent.class));
        verify(accountSummaryHandler, times(1)).handleAsync(any(MoneyDepositedEvent.class));
        verify(transactionHandler, times(1)).handleAsync(any(MoneyDepositedEvent.class));
    }
    
    @Test
    void shouldHandleEmptyEventList() throws Exception {
        CompletableFuture<Void> future = eventProcessor.processAccountEventsAsync(List.of());
        future.get(1, TimeUnit.SECONDS);
        
        verifyNoInteractions(accountSummaryHandler, transactionHandler);
    }
}