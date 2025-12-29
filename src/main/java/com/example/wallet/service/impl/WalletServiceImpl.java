package com.example.wallet.service.impl;

import com.example.wallet.controller.dto.WalletRequest;
import com.example.wallet.controller.dto.WalletResponse;
import com.example.wallet.exception.InsufficientFundsException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.model.OperationType;
import com.example.wallet.model.Wallet;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService
{
    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);
    private final WalletRepository walletRepository;

    public WalletServiceImpl(WalletRepository walletRepository)
    {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public WalletResponse processTransaction(WalletRequest request) {
        log.debug("Processing transaction: walletId={}, operation={}, amount={}",
                  request.getWalletId(), request.getOperationType(), request.getAmount());

        // Используем пессимистичную блокировку SELECT FOR UPDATE
        Wallet wallet = walletRepository.findByIdWithLock(request.getWalletId())
                .orElseGet(() -> {
                    if (request.getOperationType() == OperationType.DEPOSIT) {
                        // Создаем новый кошелек с указанным ID
                        Wallet newWallet = new Wallet(request.getWalletId(), BigDecimal.ZERO);
                        log.info("Creating new wallet: {}", request.getWalletId());
                        return walletRepository.save(newWallet);
                    } else {
                        throw new WalletNotFoundException("Wallet not found: " + request.getWalletId());
                    }
                });

        // Выполняем операцию
        BigDecimal newBalance;
        if (request.getOperationType() == OperationType.DEPOSIT)
        {
            newBalance = wallet.getBalance().add(request.getAmount());
            log.debug("Deposit: walletId={}, oldBalance={}, amount={}, newBalance={}",
                      wallet.getId(), wallet.getBalance(), request.getAmount(), newBalance);
        }
        else
        {
            // WITHDRAW
            if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                log.warn("Insufficient funds: walletId={}, balance={}, required={}",
                         wallet.getId(), wallet.getBalance(), request.getAmount());
                throw new InsufficientFundsException(
                        String.format("Insufficient funds. Current balance: %s, Required: %s",
                                      wallet.getBalance(), request.getAmount())
                );
            }

            newBalance = wallet.getBalance().subtract(request.getAmount());
            log.debug("Withdraw: walletId={}, oldBalance={}, amount={}, newBalance={}",
                      wallet.getId(), wallet.getBalance(), request.getAmount(), newBalance);
        }

        wallet.setBalance(newBalance);
        Wallet savedWallet = walletRepository.save(wallet);

        return new WalletResponse(savedWallet.getId(), savedWallet.getBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getBalance(UUID walletId)
    {
        log.debug("Getting balance for walletId={}", walletId);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> {
                    log.warn("Wallet not found: {}", walletId);
                    return new WalletNotFoundException("Wallet not found: " + walletId);
                });

        return new WalletResponse(wallet.getId(), wallet.getBalance());
    }
}