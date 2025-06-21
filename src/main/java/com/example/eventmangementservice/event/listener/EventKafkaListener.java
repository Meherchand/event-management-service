package com.example.eventmangementservice.event.listener;

import com.example.eventmangementservice.event.model.Event;
import com.example.eventmangementservice.event.search.EventSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventKafkaListener {

    private final EventSearchService eventSearchService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @KafkaListener(topics = "events", groupId = "event-search-group")
    public void handleEventMessage(String eventJson) {
        log.info("Received event message: {}", eventJson);
        
        String eventType = objectMapper.readTree(eventJson).get("eventType").asText();
        String payload = objectMapper.readTree(eventJson).get("payload").toString();
        
        switch (eventType) {
            case "EVENT_CREATED", "EVENT_UPDATED", "EVENT_PUBLISHED" -> {
                Event event = objectMapper.readValue(payload, Event.class);
                eventSearchService.indexEvent(event);
                log.info("Indexed event: {}", event.getId());
            }
            case "EVENT_DELETED" -> {
                Event event = objectMapper.readValue(payload, Event.class);
                eventSearchService.deleteIndexedEvent(event.getId());
                log.info("Deleted event from index: {}", event.getId());
            }
            default -> log.warn("Unknown event type: {}", eventType);
        }
    }
}
