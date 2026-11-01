package com.tommy.wallet.wallet;

import java.util.UUID;

public record Wallet(
        UUID userId,
        long balance,
        String currency
) {
}