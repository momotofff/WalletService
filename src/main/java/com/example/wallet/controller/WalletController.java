package com.example.wallet.controller;

import com.example.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController
{
    private final WalletService walletService;
}