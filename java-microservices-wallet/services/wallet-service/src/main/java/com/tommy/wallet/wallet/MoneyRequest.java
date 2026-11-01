package com.tommy.wallet.wallet;

import tools.jackson.databind.JsonNode;

public record MoneyRequest(JsonNode amount) {

    public long validatedAmount() {
        if (amount == null
                || !amount.isIntegralNumber()
                || !amount.canConvertToLong()) {
            throw new WalletOperationException(
                    "VALIDATION_ERROR",
                    "Amount phải là số nguyên từ 1 đến 1.000.000.000 VND."
            );
        }

        long value = amount.longValue();

        if (value < 1 || value > 1_000_000_000L) {
            throw new WalletOperationException(
                    "VALIDATION_ERROR",
                    "Amount phải là số nguyên từ 1 đến 1.000.000.000 VND."
            );
        }

        return value;
    }
}