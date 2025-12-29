package com.example.wallet.integration;

import com.example.wallet.controller.dto.WalletRequest;
import com.example.wallet.model.OperationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WalletControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testWalletId;

    @BeforeEach
    void setUp() {
        testWalletId = UUID.randomUUID();
    }

    @Test
    void deposit_NewWallet_ShouldCreateAndReturnBalance() throws Exception {
        // Arrange
        WalletRequest request = new WalletRequest(
                testWalletId,
                OperationType.DEPOSIT,
                new BigDecimal("1000.00")
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId", is(testWalletId.toString())))
                .andExpect(jsonPath("$.balance", is(1000.00)));
    }

    @Test
    void deposit_ExistingWallet_ShouldIncreaseBalance() throws Exception {
        // Arrange - первый депозит
        WalletRequest deposit1 = new WalletRequest(
                testWalletId,
                OperationType.DEPOSIT,
                new BigDecimal("500.00")
        );

        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deposit1)))
                .andExpect(status().isOk());

        // Act - второй депозит
        WalletRequest deposit2 = new WalletRequest(
                testWalletId,
                OperationType.DEPOSIT,
                new BigDecimal("300.00")
        );

        // Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deposit2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(800.00)));
    }

    @Test
    void withdraw_SufficientFunds_ShouldDecreaseBalance() throws Exception {
        // Arrange - депозит
        WalletRequest deposit = new WalletRequest(
                testWalletId,
                OperationType.DEPOSIT,
                new BigDecimal("1000.00")
        );

        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deposit)))
                .andExpect(status().isOk());

        // Act - вывод
        WalletRequest withdraw = new WalletRequest(
                testWalletId,
                OperationType.WITHDRAW,
                new BigDecimal("400.00")
        );

        // Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(withdraw)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(600.00)));
    }

    @Test
    void withdraw_InsufficientFunds_ShouldReturnBadRequest() throws Exception {
        // Arrange - депозит
        WalletRequest deposit = new WalletRequest(
                testWalletId,
                OperationType.DEPOSIT,
                new BigDecimal("100.00")
        );

        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deposit)))
                .andExpect(status().isOk());

        // Act & Assert - попытка вывести больше чем есть
        WalletRequest withdraw = new WalletRequest(
                testWalletId,
                OperationType.WITHDRAW,
                new BigDecimal("200.00")
        );

        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(withdraw)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Insufficient funds")));
    }

    @Test
    void withdraw_NonExistentWallet_ShouldReturnNotFound() throws Exception {
        // Arrange
        WalletRequest request = new WalletRequest(
                UUID.randomUUID(),
                OperationType.WITHDRAW,
                new BigDecimal("100.00")
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Wallet not found")));
    }

    @Test
    void getBalance_ExistingWallet_ShouldReturnBalance() throws Exception {
        // Arrange - создаем кошелек
        WalletRequest deposit = new WalletRequest(
                testWalletId,
                OperationType.DEPOSIT,
                new BigDecimal("750.50")
        );

        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deposit)))
                .andExpect(status().isOk());

        // Act & Assert - проверяем баланс
        mockMvc.perform(get("/api/v1/wallets/{walletId}", testWalletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId", is(testWalletId.toString())))
                .andExpect(jsonPath("$.balance", is(750.50)));
    }

    @Test
    void getBalance_NonExistentWallet_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/wallets/{walletId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Wallet not found")));
    }

    @Test
    void processTransaction_InvalidJson_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid JSON")));
    }

    @Test
    void processTransaction_MissingRequiredFields_ShouldReturnValidationError() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation failed")));
    }

    @Test
    void processTransaction_NegativeAmount_ShouldReturnValidationError() throws Exception {
        // Arrange
        String request = String.format("""
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": -100.00
            }
            """, UUID.randomUUID());

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation failed")));
    }

    @Test
    void processTransaction_ZeroAmount_ShouldReturnValidationError() throws Exception {
        // Arrange
        String request = String.format("""
            {
                "walletId": "%s",
                "operationType": "DEPOSIT",
                "amount": 0.00
            }
            """, UUID.randomUUID());

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation failed")));
    }

    @Test
    void processTransaction_InvalidUUID_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String request = """
            {
                "walletId": "invalid-uuid",
                "operationType": "DEPOSIT",
                "amount": 100.00
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid JSON")));
    }
}