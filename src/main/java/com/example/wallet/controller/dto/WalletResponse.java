package com.example.wallet.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public class WalletResponse {
    private final UUID walletId;
    private final BigDecimal balance;

    @JsonCreator
    public WalletResponse(
            @JsonProperty("walletId") UUID walletId,
            @JsonProperty("balance") BigDecimal balance) {
        this.walletId = walletId;
        this.balance = balance;
    }

    // Getters
    public UUID getWalletId() { return walletId; }
    public BigDecimal getBalance() { return balance; }
}