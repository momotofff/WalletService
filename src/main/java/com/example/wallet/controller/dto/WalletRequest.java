package com.example.wallet.controller.dto;

import com.example.wallet.model.OperationType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public class WalletRequest
{
    @NotNull(message = "walletId is required")
    private final UUID walletId;

    @NotNull(message = "operationType is required")
    private final OperationType operationType;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private final BigDecimal amount;

    @JsonCreator
    public WalletRequest(
            @JsonProperty("walletId") UUID walletId,
            @JsonProperty("operationType") OperationType operationType,
            @JsonProperty("amount") BigDecimal amount)
    {
        this.walletId = walletId;
        this.operationType = operationType;
        this.amount = amount;
    }

    // Getters
    public UUID getWalletId() { return walletId; }
    public OperationType getOperationType() { return operationType; }
    public BigDecimal getAmount() { return amount; }
}