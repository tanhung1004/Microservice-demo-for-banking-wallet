package com.tommy.subscriber.messaging;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class DepositEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(DepositEventListener.class);

    private final ObjectMapper objectMapper;

    public DepositEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.deposit-topic}")
    public void consume(ConsumerRecord<String, String> record) {
        JsonNode event;
        UUID eventId;
        UUID transactionId;
        UUID userId;
        long amount;
        long balanceAfter;

        try {
            if (record.value() == null) {
                throw new IllegalArgumentException("EMPTY_VALUE");
            }

            event = objectMapper.readTree(record.value());

            if (event == null || !event.isObject()) {
                throw new IllegalArgumentException("EXPECTED_JSON_OBJECT");
            }

            if (!"MoneyDeposited".equals(text(event, "eventType"))) {
                throw new IllegalArgumentException("UNSUPPORTED_EVENT_TYPE");
            }

            if (integer(event, "schemaVersion") != 1) {
                throw new IllegalArgumentException("UNSUPPORTED_SCHEMA_VERSION");
            }

            eventId = uuid(event, "eventId");
            transactionId = uuid(event, "transactionId");
            userId = uuid(event, "userId");

            if (!userId.toString().equals(record.key())) {
                throw new IllegalArgumentException("KEY_USER_ID_MISMATCH");
            }

            amount = integer(event, "amount");
            balanceAfter = integer(event, "balanceAfter");

            if (amount < 1 || amount > 1_000_000_000L) {
                throw new IllegalArgumentException("AMOUNT_OUT_OF_RANGE");
            }

            if (balanceAfter < amount
                    || balanceAfter > 1_000_000_000_000L) {
                throw new IllegalArgumentException("INVALID_BALANCE_AFTER");
            }

            if (!"VND".equals(text(event, "currency"))) {
                throw new IllegalArgumentException("UNSUPPORTED_CURRENCY");
            }

            validateOccurredAt(text(event, "occurredAt"));

        } catch (JacksonException exception) {
            logInvalid(record, "INVALID_JSON");
            return;
        } catch (IllegalArgumentException exception) {
            // Các message bên trên là lý do cố định, không chứa payload.
            logInvalid(record, exception.getMessage());
            return;
        }

        // Chỉ ghi các trường đã kiểm tra, không cập nhật database.
        log.info(
                "MoneyDeposited received: eventId={}, transactionId={}, "
                        + "userId={}, amount={}, balanceAfter={}, currency=VND, "
                        + "partition={}, offset={}",
                eventId,
                transactionId,
                userId,
                amount,
                balanceAfter,
                record.partition(),
                record.offset()
        );
    }

    private String text(JsonNode event, String field) {
        JsonNode value = event.get(field);

        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("INVALID_FIELD_" + field);
        }

        return value.stringValue();
    }

    private long integer(JsonNode event, String field) {
        JsonNode value = event.get(field);

        if (value == null
                || !value.isIntegralNumber()
                || !value.canConvertToLong()) {
            throw new IllegalArgumentException("INVALID_FIELD_" + field);
        }

        return value.longValue();
    }

    private UUID uuid(JsonNode event, String field) {
        String value = text(event, field);

        try {
            UUID parsed = UUID.fromString(value);

            if (!parsed.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException();
            }

            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("INVALID_UUID_" + field);
        }
    }

    private void validateOccurredAt(String value) {
        if (!value.endsWith("Z")) {
            throw new IllegalArgumentException("OCCURRED_AT_MUST_BE_UTC");
        }

        try {
            Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("INVALID_OCCURRED_AT");
        }
    }

    private void logInvalid(
            ConsumerRecord<String, String> record,
            String reason
    ) {
        log.warn(
                "Invalid deposit event skipped: topic={}, partition={}, "
                        + "offset={}, reason={}",
                record.topic(),
                record.partition(),
                record.offset(),
                reason
        );
    }
}