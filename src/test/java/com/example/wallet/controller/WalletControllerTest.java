package com.example.wallet.controller;

import com.example.wallet.controller.dto.WalletRequest;
import com.example.wallet.controller.dto.WalletResponse;
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
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @Test
    void processTransaction_ValidRequest_ShouldReturnOk() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();
        WalletRequest request = new WalletRequest(
                walletId,
                OperationType.DEPOSIT,
                new BigDecimal("1000.00")
        );

        WalletResponse response = new WalletResponse(walletId, new BigDecimal("1000.00"));

        when(walletService.processTransaction(any(WalletRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void getBalance_ValidWalletId_ShouldReturnOk() throws Exception {
        // Arrange
        UUID walletId = UUID.randomUUID();
        WalletResponse response = new WalletResponse(walletId, new BigDecimal("500.00"));

        when(walletService.getBalance(walletId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(500.00));
    }
}