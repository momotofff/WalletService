package com.example.wallet.service;

import com.example.wallet.controller.dto.WalletRequest;
import com.example.wallet.controller.dto.WalletResponse;

import java.util.UUID;

public interface WalletService {
    WalletResponse processTransaction(WalletRequest request);
    WalletResponse getBalance(UUID walletId);
}