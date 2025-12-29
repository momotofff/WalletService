package com.example.wallet.controller;

import com.example.wallet.controller.dto.WalletRequest;
import com.example.wallet.controller.dto.WalletResponse;
import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WalletController
{

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/wallet")
    public ResponseEntity<WalletResponse> processTransaction(@Valid @RequestBody WalletRequest request)
    {
        WalletResponse response = walletService.processTransaction(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletResponse> getBalance(@PathVariable UUID walletId)
    {
        WalletResponse response = walletService.getBalance(walletId);
        return ResponseEntity.ok(response);
    }
}