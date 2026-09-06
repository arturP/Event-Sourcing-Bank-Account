package io.artur.bankaccount.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies real Spring input-port wiring, without mocks or a manually assembled controller. */
@SpringBootTest(properties = "db.url=jdbc:h2:mem:input-ports;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@WithMockUser
class AccountInputPortsIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateDepositAndReadAccountThroughInputPorts() throws Exception {
        String response = mockMvc.perform(post("/api/accounts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountHolderName":"Jane Smith","overdraftLimit":100.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String accountId = objectMapper.readTree(response).get("accountId").asText();

        mockMvc.perform(post("/api/accounts/{id}/deposit", accountId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":25.00}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountHolderName").value("Jane Smith"))
                .andExpect(jsonPath("$.balance").value(25.00))
                .andExpect(jsonPath("$.availableBalance").value(125.00));
    }
}
