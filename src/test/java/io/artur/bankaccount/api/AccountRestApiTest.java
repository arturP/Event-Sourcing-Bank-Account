package io.artur.bankaccount.api;

import io.artur.bankaccount.api.controller.AccountController;
import io.artur.bankaccount.application.ports.incoming.AccountManagementUseCase;
import io.artur.bankaccount.application.ports.incoming.AccountQueryUseCase;
import io.artur.bankaccount.application.ports.incoming.AccountSummaryQueryUseCase;
import io.artur.bankaccount.application.ports.incoming.TransactionQueryUseCase;
import io.artur.bankaccount.application.queries.readmodels.AccountDetailsResult;
import io.artur.bankaccount.application.queries.readmodels.AccountActionResult;
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
    private AccountManagementUseCase applicationService;

    @MockBean
    private AccountQueryUseCase accountQueryUseCase;
    
    @MockBean
    private AccountSummaryQueryUseCase accountQueryHandler;
    
    @MockBean
    private TransactionQueryUseCase transactionQueryHandler;
    
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
        AccountDetailsResult account = new AccountDetailsResult(accountId, "Jane Smith",
                BigDecimal.valueOf(250), BigDecimal.valueOf(500), BigDecimal.valueOf(750), "ACTIVE");

        when(accountQueryUseCase.findAccountById(accountId)).thenReturn(Optional.of(account));
        
        // When & Then
        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.accountHolderName").value("Jane Smith"))
                .andExpect(jsonPath("$.balance").value(250))
                .andExpect(jsonPath("$.availableBalance").value(750)); // 250 + 500 overdraft
        
        verify(accountQueryUseCase).findAccountById(accountId);
    }
    
    @Test
    void shouldReturnNotFoundForNonexistentAccount() throws Exception {
        // Given
        UUID accountId = UUID.randomUUID();
        when(accountQueryUseCase.findAccountById(accountId)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isNotFound());
        
        verify(accountQueryUseCase).findAccountById(accountId);
    }

    @Test
    void shouldUseActionDecisionReturnedByTheInputPort() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountQueryUseCase.canPerformAction(accountId, "FREEZE"))
                .thenReturn(Optional.of(new AccountActionResult(accountId, "FREEZE", false,
                        "Already frozen", new BigDecimal("25.00"), "FROZEN")));

        mockMvc.perform(get("/api/accounts/{id}/can-perform/FREEZE", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canPerform").value(false))
                .andExpect(jsonPath("$.reason").value("Already frozen"))
                .andExpect(jsonPath("$.accountStatus").value("FROZEN"));
        verify(accountQueryUseCase).canPerformAction(accountId, "FREEZE");
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
