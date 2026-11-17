package com.renauldiedev.event.infrastructure.kafka;

import com.renauldiedev.event.domain.order.Order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renauldiedev.event.infrastructure.kafka.KafkaTopicConfig;
import com.renauldiedev.event.infrastructure.kafka.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC, groupId = "event-consumer-group")
    public void consumeOrderEvent(
            @Payload String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received order event from Kafka topic [{}]: partition={}, offset={}, key={}",
                KafkaTopicConfig.ORDER_EVENTS_TOPIC, partition, offset, key);

        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
            log.info("Processing order event: orderNumber=[{}], userId=[{}], totalAmount=[{}], quantity=[{}]",
                    event.getOrderNumber(), event.getUserId(), event.getTotalAmount(), event.getQuantity());

            log.info("Order event [{}] processed successfully by consumer.", event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to process order event key [{}]: {}. Delegating to retry policy...", key, e.getMessage());
            throw new RuntimeException("Failed processing order event for key: " + key, e);
        }
    }

    @DltHandler
    public void handleDeadLetterMessage(
            ConsumerRecord<String, String> record,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
        log.error("CRITICAL: Message permanently failed and forwarded to Dead Letter Topic [{}]! Partition: {}, Offset: {}, Key: [{}], Value: [{}], Error: [{}]",
                topic, record.partition(), record.offset(), record.key(), record.value(), exceptionMessage);
    }
}
