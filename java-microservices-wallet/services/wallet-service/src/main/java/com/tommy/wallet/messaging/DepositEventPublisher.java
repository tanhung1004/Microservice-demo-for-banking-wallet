package com.tommy.wallet.messaging;

import com.tommy.wallet.event.MoneyDepositedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import tools.jackson.databind.ObjectMapper;

@Component
public class DepositEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(DepositEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public DepositEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.deposit-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    // Chỉ được gọi khi transaction DB đã commit thành công.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(MoneyDepositedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, event.userId().toString(), json)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            logPublishFailure(event, exception);
                            return;
                        }

                        log.info(
                                "Deposit event published: eventId={}, transactionId={}, topic={}",
                                event.eventId(),
                                event.transactionId(),
                                topic
                        );
                    });
        } catch (Exception exception) {
            // DB đã commit: không ném lỗi khiến API báo cộng tiền thất bại.
            logPublishFailure(event, exception);
        }
    }

    private void logPublishFailure(
            MoneyDepositedEvent event,
            Throwable exception
    ) {
        log.error(
                "Deposit event publish failed AFTER DB COMMIT: "
                        + "eventId={}, transactionId={}, topic={}, errorType={}. "
                        + "Balance remains updated; event delivery is not guaranteed.",
                event.eventId(),
                event.transactionId(),
                topic,
                exception.getClass().getName()
        );
    }
}