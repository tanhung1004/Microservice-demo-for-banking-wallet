package com.tommy.wallet.wallet;

import java.util.UUID;

public record MoneyResponse(
        UUID transactionId,
        String type,
        long amount,
        long balance,
        String currency
) {
}