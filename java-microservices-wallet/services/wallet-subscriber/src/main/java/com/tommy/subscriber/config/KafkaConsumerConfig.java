package com.tommy.subscriber.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Deposit event skipped after retries: "
                                + "topic={}, partition={}, offset={}, errorType={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        exception.getClass().getName()
                ),
                new FixedBackOff(1000L, 2L)
        );
    }
}