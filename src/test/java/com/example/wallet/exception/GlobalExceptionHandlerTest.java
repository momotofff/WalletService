package com.example.wallet.exception;

import com.example.wallet.controller.WalletController;
import com.example.wallet.controller.dto.WalletRequest;
import com.example.wallet.model.OperationType;
import com.example.wallet.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @Test
    void handleWalletNotFoundException_ShouldReturnNotFound() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();
        WalletRequest request = new WalletRequest(
                walletId,
                OperationType.WITHDRAW,
                new BigDecimal("100.00")
        );

        when(walletService.processTransaction(any(WalletRequest.class)))
                .thenThrow(new WalletNotFoundException("Wallet not found: " + walletId));

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet not found"))
                .andExpect(jsonPath("$.message").value("Wallet not found: " + walletId))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void handleInsufficientFundsException_ShouldReturnBadRequest() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();
        WalletRequest request = new WalletRequest(
                walletId,
                OperationType.WITHDRAW,
                new BigDecimal("1000.00")
        );

        when(walletService.processTransaction(any(WalletRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds. Current balance: 500.00"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient funds"))
                .andExpect(jsonPath("$.message").value("Insufficient funds. Current balance: 500.00"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void handleInvalidJson_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid JSON"));
    }

    @Test
    void handleValidationException_MissingFields_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void handleInvalidUUID_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String invalidRequest = """
            {
                "walletId": "not-a-uuid",
                "operationType": "DEPOSIT",
                "amount": 100.00
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid JSON"));
    }
}