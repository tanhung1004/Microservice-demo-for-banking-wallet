package com.tommy.wallet.event;

import java.util.UUID;

public record MoneyDepositedEvent(
        UUID eventId,
        String eventType,
        int schemaVersion,
        UUID transactionId,
        UUID userId,
        long amount,
        long balanceAfter,
        String currency,
        String occurredAt
) {
}