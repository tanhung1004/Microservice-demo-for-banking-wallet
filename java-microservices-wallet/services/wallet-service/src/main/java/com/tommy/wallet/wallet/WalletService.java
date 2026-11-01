package com.tommy.wallet.wallet;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet getBalance(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);
    }
}