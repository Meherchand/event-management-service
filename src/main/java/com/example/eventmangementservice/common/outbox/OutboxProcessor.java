package com.example.eventmangementservice.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void processOutboxMessages() {
        outboxRepository.findUnprocessedMessages(PageRequest.of(0, 10))
                .forEach(this::processMessage);
    }

    private void processMessage(OutboxMessage message) {
        try {
            message.setStatus(OutboxStatus.PROCESSING);
            outboxRepository.save(message);

            // This is where the topic is determined based on aggregate type
            String topicName = determineTopicName(message.getAggregateType());
            
            // This is where the message is sent to the Kafka topic
            kafkaTemplate.send(topicName, message.getAggregateId(), message.getPayload())
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            markAsProcessed(message.getId());
                            log.info("Successfully sent message to Kafka: {}", message.getId());
                        } else {
                            handleFailure(message.getId(), exception);
                        }
                    });
        } catch (Exception e) {
            handleFailure(message.getId(), e);
        }
    }

    @Transactional
    public void markAsProcessed(java.util.UUID id) {
        outboxRepository.markAsProcessed(id, LocalDateTime.now());
    }

    @Transactional
    public void handleFailure(java.util.UUID id, Throwable exception) {
        log.error("Failed to process outbox message {}: {}", id, exception.getMessage());
        outboxRepository.updateRetryCount(id, OutboxStatus.FAILED);
    }

    // This method maps aggregate types to Kafka topics
    private String determineTopicName(String aggregateType) {
        return switch (aggregateType.toUpperCase()) {
            case "EVENT" -> "events";
            case "BOOKING" -> "bookings";
            case "PAYMENT" -> "payments";
            default -> "notifications";
        };
    }
}
