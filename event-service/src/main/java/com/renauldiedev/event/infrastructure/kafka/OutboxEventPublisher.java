package com.renauldiedev.event.infrastructure.kafka;

import com.renauldiedev.event.infrastructure.kafka.KafkaTopicConfig;
import com.renauldiedev.event.domain.outbox.OutboxEvent;
import com.renauldiedev.event.domain.outbox.OutboxStatus;
import com.renauldiedev.event.domain.outbox.OutboxEventRepository;
import com.renauldiedev.event.domain.outbox.OutboxStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OutboxEventPublisher {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxStatusRepository outboxStatusRepository;
    private final KafkaProducerService kafkaProducerService;

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxStatusRepository outboxStatusRepository,
            KafkaProducerService kafkaProducerService) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxStatusRepository = outboxStatusRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Scheduled(fixedRate = 5000)
    public void publishPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusNameOrderByCreatedAtAsc(
                "PENDING", PageRequest.of(0, 50));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox event(s) to publish.", pendingEvents.size());

        OutboxStatus processedStatus = outboxStatusRepository.findByName("PROCESSED").orElse(null);
        OutboxStatus failedStatus = outboxStatusRepository.findByName("FAILED").orElse(null);

        for (OutboxEvent event : pendingEvents) {
            publishSingleEvent(event, processedStatus, failedStatus);
        }
    }

    @Transactional
    public void publishSingleEvent(OutboxEvent event, OutboxStatus processedStatus, OutboxStatus failedStatus) {
        try {

            kafkaProducerService.sendEvent(KafkaTopicConfig.ORDER_EVENTS_TOPIC, event.getAggregateId(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);

            if (processedStatus != null) {
                event.setStatus(processedStatus);
            }
            event.setProcessedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
            log.info("Outbox event [{}] successfully published and marked as PROCESSED.", event.getId());
        } catch (Exception e) {
            int newRetryCount = event.getRetryCount() + 1;
            event.setRetryCount(newRetryCount);
            log.warn("Failed to publish outbox event [{}] to Kafka (attempt {}/{}): {}",
                    event.getId(), newRetryCount, MAX_RETRY_ATTEMPTS, e.getMessage());

            if (newRetryCount >= MAX_RETRY_ATTEMPTS) {
                if (failedStatus != null) {
                    event.setStatus(failedStatus);
                }
                log.error("Outbox event [{}] exceeded max retry attempts. Marked as FAILED.", event.getId());
            }

            outboxEventRepository.save(event);
        }
    }
}
