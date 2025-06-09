package io.artur.bankaccount.api;

import io.artur.bankaccount.api.controller.AccountController;
import io.artur.bankaccount.application.services.AccountApplicationService;
import io.artur.bankaccount.application.queries.handlers.AccountQueryHandler;
import io.artur.bankaccount.application.queries.handlers.TransactionQueryHandler;
import io.artur.bankaccount.domain.account.aggregates.BankAccount;
import io.artur.bankaccount.domain.shared.events.EventMetadata;
import io.artur.bankaccount.domain.shared.valueobjects.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API tests for Account Controller using MockMvc
 * Tests the web layer without full application context startup
 */
@WebMvcTest(controllers = AccountController.class)
@TestPropertySource(properties = {
    "bankaccount.infrastructure.native.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class AccountRestApiTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AccountApplicationService applicationService;
    
    @MockBean
    private AccountQueryHandler accountQueryHandler;
    
    @MockBean
    private TransactionQueryHandler transactionQueryHandler;
    
    @Test
    void shouldCreateAccountSuccessfully() throws Exception {
        // Given
        UUID accountId = UUID.randomUUID();
        when(applicationService.openAccount(any())).thenReturn(accountId);
        
        String requestBody = """
            {
                "accountHolderName": "John Doe",
                "overdraftLimit": 1000.00
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$.balance").value(1000))
                .andExpect(jsonPath("$.availableBalance").value(1000));
        
        verify(applicationService).openAccount(any());
    }
    
    @Test
    void shouldReturnAccountDetails() throws Exception {
        // Given
        UUID accountId = UUID.randomUUID();
        EventMetadata metadata = new EventMetadata(1);
        
        BankAccount account = BankAccount.openNewAccount(
            accountId,
            "Jane Smith", 
            BigDecimal.valueOf(500), 
            metadata
        );
        account.deposit(BigDecimal.valueOf(250), metadata);
        
        when(applicationService.findAccountById(accountId)).thenReturn(Optional.of(account));
        
        // When & Then
        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.accountHolderName").value("Jane Smith"))
                .andExpect(jsonPath("$.balance").value(250))
                .andExpect(jsonPath("$.availableBalance").value(750)); // 250 + 500 overdraft
        
        verify(applicationService).findAccountById(accountId);
    }
    
    @Test
    void shouldReturnNotFoundForNonexistentAccount() throws Exception {
        // Given
        UUID accountId = UUID.randomUUID();
        when(applicationService.findAccountById(accountId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isNotFound());
        
        verify(applicationService).findAccountById(accountId);
    }
    
    @Test
    void shouldProcessDepositSuccessfully() throws Exception {
        // Given
        UUID accountId = UUID.randomUUID();
        doNothing().when(applicationService).deposit(any());
        
        String requestBody = """
            {
                "amount": 150.00,
                "description": "Salary deposit"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/accounts/{accountId}/deposit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Deposit completed successfully"))
                .andExpect(jsonPath("$.amount").value(150));
        
        verify(applicationService).deposit(any());
    }
    
    @Test
    void shouldProcessWithdrawalSuccessfully() throws Exception {
        // Given
        UUID accountId = UUID.randomUUID();
        doNothing().when(applicationService).withdraw(any());
        
        String requestBody = """
            {
                "amount": 75.00,
                "description": "ATM withdrawal"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Withdrawal completed successfully"))
                .andExpect(jsonPath("$.amount").value(75));
        
        verify(applicationService).withdraw(any());
    }
    
    @Test
    void shouldHandleWithdrawalFailure() throws Exception {
        // Given
        UUID accountId = UUID.randomUUID();
        doThrow(new RuntimeException("Insufficient funds")).when(applicationService).withdraw(any());
        
        String requestBody = """
            {
                "amount": 1000.00,
                "description": "Large withdrawal"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Insufficient funds"))
                .andExpect(jsonPath("$.amount").value(1000));
        
        verify(applicationService).withdraw(any());
    }
    
    @Test
    void shouldProcessTransferSuccessfully() throws Exception {
        // Given
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        doNothing().when(applicationService).transfer(any());
        
        String requestBody = """
            {
                "amount": 200.00,
                "description": "Payment to friend"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/accounts/{fromAccountId}/transfer/{toAccountId}", fromAccountId, toAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Transfer completed successfully"))
                .andExpect(jsonPath("$.amount").value(200));
        
        verify(applicationService).transfer(any());
    }
    
    @Test
    void shouldHandleTransferFailure() throws Exception {
        // Given
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        doThrow(new RuntimeException("Account not found")).when(applicationService).transfer(any());
        
        String requestBody = """
            {
                "amount": 100.00,
                "description": "Failed transfer"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/accounts/{fromAccountId}/transfer/{toAccountId}", fromAccountId, toAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Account not found"))
                .andExpect(jsonPath("$.amount").value(100));
        
        verify(applicationService).transfer(any());
    }
}