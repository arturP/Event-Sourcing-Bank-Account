package io.artur.bankaccount.api;

import io.artur.bankaccount.api.controller.AccountController;
import io.artur.bankaccount.application.ports.outgoing.AccountRepository;
import io.artur.bankaccount.application.ports.outgoing.CachePort;
import io.artur.bankaccount.application.ports.outgoing.EventStorePort;
import io.artur.bankaccount.application.ports.outgoing.MetricsPort;
import io.artur.bankaccount.application.services.AccountApplicationService;
import io.artur.bankaccount.infrastructure.monitoring.NativeMetricsCollector;
import io.artur.bankaccount.infrastructure.persistence.cache.NativeCacheService;
import io.artur.bankaccount.infrastructure.persistence.eventstore.NativeEventStore;
import io.artur.bankaccount.infrastructure.persistence.eventstore.serialization.EventSerializer;
import io.artur.bankaccount.infrastructure.persistence.repositories.NativeAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Complete end-to-end API integration test using native infrastructure
 * Tests the full stack: REST API -> Application Service -> Native Infrastructure -> Database
 */
class NativeInfrastructureApiIntegrationTest {
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AccountApplicationService applicationService;
    private EventStorePort eventStorePort;
    private CachePort cachePort;
    private MetricsPort metricsPort;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create in-memory H2 database with unique name for each test
        DataSource dataSource = createTestDataSource();
        
        // Create native infrastructure components
        EventSerializer eventSerializer = new EventSerializer();
        eventStorePort = new NativeEventStore(dataSource, eventSerializer);
        cachePort = new NativeCacheService();
        metricsPort = new NativeMetricsCollector();
        
        AccountRepository accountRepository = new NativeAccountRepository(eventStorePort);
        
        // Create application service with native infrastructure
        applicationService = new AccountApplicationService(accountRepository, cachePort, metricsPort);
        
        // Create controller and MockMvc
        AccountController controller = new AccountController(applicationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }
    
    private DataSource createTestDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        // Use unique database name for each test to avoid conflicts
        String dbName = "testdb_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
        dataSource.setUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setDriverClassName("org.h2.Driver");
        
        // Initialize database schema
        initializeSchema(dataSource);
        
        return dataSource;
    }
    
    private void initializeSchema(DataSource dataSource) {
        String createEventsTable = """
            CREATE TABLE IF NOT EXISTS events (
                event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                aggregate_id VARCHAR(36) NOT NULL,
                event_type VARCHAR(100) NOT NULL,
                event_data CLOB NOT NULL,
                event_version BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL,
                correlation_id VARCHAR(36)
            )
            """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createEventsTable);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
    
    @Test
    void shouldPerformCompleteAccountLifecycleWithNativeInfrastructure() throws Exception {
        // Step 1: Create Account
        String createAccountRequest = """
            {
                "accountHolderName": "Integration Test User",
                "overdraftLimit": 500.00
            }
            """;
        
        String createResponse = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAccountRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountHolderName").value("Integration Test User"))
                .andExpect(jsonPath("$.balance").value(500.0)) // Initial overdraft limit as balance
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Extract account ID from response
        UUID accountId = UUID.fromString(objectMapper.readTree(createResponse).get("accountId").asText());
        
        // Step 2: Verify account exists and has correct initial state
        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.accountHolderName").value("Integration Test User"))
                .andExpect(jsonPath("$.balance").value(0.0)) // Actual balance should be 0
                .andExpect(jsonPath("$.availableBalance").value(500.0)); // Available = balance + overdraft
        
        // Step 3: Make a deposit
        String depositRequest = """
            {
                "amount": 1000.00,
                "description": "Initial deposit"
            }
            """;
        
        mockMvc.perform(post("/api/accounts/{accountId}/deposit", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(depositRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(1000.0));
        
        // Step 4: Verify balance after deposit
        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.0))
                .andExpect(jsonPath("$.availableBalance").value(1500.0)); // 1000 + 500 overdraft
        
        // Step 5: Make a withdrawal
        String withdrawalRequest = """
            {
                "amount": 300.00,
                "description": "ATM withdrawal"
            }
            """;
        
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(withdrawalRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(300.0));
        
        // Step 6: Verify balance after withdrawal
        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.0))
                .andExpect(jsonPath("$.availableBalance").value(1200.0)); // 700 + 500 overdraft
        
        // Step 7: Verify events were persisted correctly
        assert eventStorePort.hasEvents(accountId) : "Account should have events";
        assert eventStorePort.getEventCount(accountId) == 3 : "Should have 3 events (open, deposit, withdraw)";
        assert eventStorePort.getLatestVersion(accountId) == 3 : "Latest version should be 3";
        
        // Step 8: Verify caching is working
        CachePort.CacheStatistics cacheStats = cachePort.getStatistics();
        assert cacheStats.getTotalRequests() > 0 : "Cache should have been accessed";
        
        // Step 9: Verify metrics are being collected
        MetricsPort.PerformanceSummary metricsSummary = metricsPort.getPerformanceSummary();
        MetricsPort.BusinessMetrics businessMetrics = metricsSummary.getBusinessMetrics();
        assert businessMetrics.getAccountsCreated() >= 1 : "Should have recorded account creation";
        assert businessMetrics.getDepositsProcessed() >= 1 : "Should have recorded deposit";
        assert businessMetrics.getWithdrawalsProcessed() >= 1 : "Should have recorded withdrawal";
    }
    
    @Test
    void shouldCreateTwoAccountsAndPerformTransfer() throws Exception {
        // Create first account
        String createAccount1Request = """
            {
                "accountHolderName": "Alice Smith",
                "overdraftLimit": 100.00
            }
            """;
        
        String response1 = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAccount1Request))
                .andDo(result -> {
                    System.out.println("Response status: " + result.getResponse().getStatus());
                    System.out.println("Response body: " + result.getResponse().getContentAsString());
                    System.out.println("Response headers: " + result.getResponse().getHeaderNames());
                })
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        UUID accountId1 = UUID.fromString(objectMapper.readTree(response1).get("accountId").asText());
        
        // Create second account
        String createAccount2Request = """
            {
                "accountHolderName": "Bob Johnson",
                "overdraftLimit": 200.00
            }
            """;
        
        String response2 = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAccount2Request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        UUID accountId2 = UUID.fromString(objectMapper.readTree(response2).get("accountId").asText());
        
        // Deposit money to first account
        String depositRequest = """
            {
                "amount": 500.00,
                "description": "Initial balance"
            }
            """;
        
        mockMvc.perform(post("/api/accounts/{accountId}/deposit", accountId1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(depositRequest))
                .andExpect(status().isOk());
        
        // Transfer money from first to second account
        String transferRequest = """
            {
                "amount": 150.00,
                "description": "Transfer to Bob"
            }
            """;
        
        mockMvc.perform(post("/api/accounts/{fromAccountId}/transfer/{toAccountId}", accountId1, accountId2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(150.0));
        
        // Verify balances after transfer
        mockMvc.perform(get("/api/accounts/{accountId}", accountId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(350.0)); // 500 - 150
        
        mockMvc.perform(get("/api/accounts/{accountId}", accountId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.0)); // 0 + 150
        
        // Verify event persistence for both accounts
        assert eventStorePort.getEventCount(accountId1) == 3 : "Account 1 should have 3 events (open, deposit, transfer-out)";
        assert eventStorePort.getEventCount(accountId2) == 2 : "Account 2 should have 2 events (open, transfer-in)";
    }
    
    @Test
    void shouldHandleErrorConditionsCorrectly() throws Exception {
        // Test: Get non-existent account
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/accounts/{accountId}", nonExistentId))
                .andExpect(status().isNotFound());
        
        // Create account for error testing
        String createAccountRequest = """
            {
                "accountHolderName": "Error Test User",
                "overdraftLimit": 50.00
            }
            """;
        
        String response = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAccountRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        UUID accountId = UUID.fromString(objectMapper.readTree(response).get("accountId").asText());
        
        // Test: Withdraw more than available balance (should exceed overdraft)
        String excessiveWithdrawalRequest = """
            {
                "amount": 100.00,
                "description": "Excessive withdrawal"
            }
            """;
        
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(excessiveWithdrawalRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
        
        // Test: Transfer from non-existent account
        String transferRequest = """
            {
                "amount": 25.00,
                "description": "Transfer from nowhere"
            }
            """;
        
        mockMvc.perform(post("/api/accounts/{fromAccountId}/transfer/{toAccountId}", nonExistentId, accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}