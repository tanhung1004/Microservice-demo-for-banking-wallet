package com.tommy.wallet.wallet;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private static final long MAX_AMOUNT = 1_000_000_000L;
    private static final long MAX_BALANCE = 1_000_000_000_000L;

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet getBalance(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);
    }

    @Transactional
    public MoneyResponse deposit(UUID userId, long amount) {
        // Bảo vệ quy tắc nghiệp vụ cả khi gọi service ngoài controller.
        if (amount < 1 || amount > MAX_AMOUNT) {
            throw new WalletOperationException(
                    "VALIDATION_ERROR",
                    "Amount phải là số nguyên từ 1 đến 1.000.000.000 VND."
            );
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(WalletNotFoundException::new);

        // Kiểm tra trước khi cộng để tránh vượt giới hạn.
        if (amount > MAX_BALANCE - wallet.balance()) {
            throw new WalletOperationException(
                    "BALANCE_LIMIT_EXCEEDED",
                    "Số dư vượt giới hạn cho phép."
            );
        }

        long newBalance = wallet.balance() + amount;
        UUID transactionId = UUID.randomUUID();

        walletRepository.updateBalance(userId, newBalance);

        return new MoneyResponse(
                transactionId,
                "DEPOSIT",
                amount,
                newBalance,
                wallet.currency()
        );
    }
}