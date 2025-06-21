package com.example.eventmangementservice.common.outbox;

import com.example.eventmangementservice.event.model.Event;
import com.example.eventmangementservice.event.model.EventDocument;
import com.example.eventmangementservice.event.service.EventSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxProcessorService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EventSearchService eventSearchService;
    
    public OutboxProcessorService(KafkaTemplate<String, String> kafkaTemplate, 
                                  ObjectMapper objectMapper,
                                  EventSearchService eventSearchService) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.eventSearchService = eventSearchService;
    }
    
    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessage> messages = entityManager
                .createQuery("SELECT o FROM OutboxMessage o WHERE o.processed = false ORDER BY o.createdAt", 
                        OutboxMessage.class)
                .setMaxResults(100)
                .getResultList();
        
        for (OutboxMessage message : messages) {
            try {
                // Process based on event type
                if (message.getAggregateType().equals("EVENT")) {
                    processEventMessage(message);
                }
                
                // Send to Kafka
                kafkaTemplate.send(
                        message.getAggregateType().toLowerCase() + "-" + message.getEventType().toLowerCase(),
                        message.getAggregateId(),
                        message.getPayload()
                );
                
                // Mark as processed
                message.setProcessed(true);
                entityManager.merge(message);
            } catch (Exception e) {
                // Log error but continue processing other messages
                System.err.println("Failed to process outbox message: " + e.getMessage());
            }
        }
    }
    
    private void processEventMessage(OutboxMessage message) throws Exception {
        Event event = objectMapper.readValue(message.getPayload(), Event.class);
        
        // Convert to EventDocument for Elasticsearch
        EventDocument eventDocument = new EventDocument();
        eventDocument.setId(event.getId().toString());
        eventDocument.setName(event.getName());
        eventDocument.setDescription(event.getDescription());
        eventDocument.setStartDate(event.getStartDate());
        eventDocument.setEndDate(event.getEndDate());
        eventDocument.setVenue(event.getVenue());
        eventDocument.setAvailableSeats(event.getAvailableSeats());
        eventDocument.setTicketPrice(event.getTicketPrice());
        eventDocument.setCategory(event.getCategory());
        eventDocument.setActive(event.isActive());
        
        // Update Elasticsearch based on event type
        switch (message.getEventType()) {
            case "EVENT_CREATED":
            case "EVENT_UPDATED":
            case "SEATS_RESERVED":
            case "SEATS_RELEASED":
                eventSearchService.indexEvent(eventDocument);
                break;
            case "EVENT_DELETED":
                eventSearchService.deleteEventIndex(event.getId().toString());
                break;
        }
    }
}
