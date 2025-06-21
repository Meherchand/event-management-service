package com.example.eventmangementservice.event.service;

import com.example.eventmangementservice.common.exception.BusinessException;
import com.example.eventmangementservice.common.exception.ResourceNotFoundException;
import com.example.eventmangementservice.common.outbox.OutboxMessage;
import com.example.eventmangementservice.common.outbox.OutboxRepository;
import com.example.eventmangementservice.common.outbox.OutboxStatus;
import com.example.eventmangementservice.event.dto.EventCreateRequest;
import com.example.eventmangementservice.event.dto.EventResponse;
import com.example.eventmangementservice.event.dto.EventSearchRequest;
import com.example.eventmangementservice.event.dto.EventUpdateRequest;
import com.example.eventmangementservice.event.model.Category;
import com.example.eventmangementservice.event.model.Event;
import com.example.eventmangementservice.event.model.EventStatus;
import com.example.eventmangementservice.event.repository.CategoryRepository;
import com.example.eventmangementservice.event.repository.EventRepository;
import com.example.eventmangementservice.event.repository.VenueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEvents(Pageable pageable) {
        return eventRepository.findByPublishedTrue(pageable)
                .map(this::mapToEventResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID id) {
        Event event = findEventById(id);
        return mapToEventResponse(event);
    }

    @Transactional
    public EventResponse createEvent(EventCreateRequest request) {
        if (!venueRepository.existsById(request.getVenueId())) {
            throw ResourceNotFoundException.of("Venue", "id", request.getVenueId());
        }
        
        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            categories = request.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId)
                            .orElseThrow(() -> ResourceNotFoundException.of("Category", "id", categoryId)))
                    .collect(Collectors.toSet());
        }
        
        Event event = new Event();
        // Map properties from request to event
        // ...
        
        Event savedEvent = eventRepository.save(event);
        createOutboxMessage("EVENT_CREATED", savedEvent);
        
        return mapToEventResponse(savedEvent);
    }

    @Transactional
    public EventResponse updateEvent(UUID id, EventUpdateRequest request) {
        Event event = findEventById(id);
        
        if (event.isPublished() && !request.isPublished()) {
            throw new BusinessException("Cannot unpublish an already published event");
        }
        
        // Update event properties
        // ...
        
        Event updatedEvent = eventRepository.save(event);
        createOutboxMessage("EVENT_UPDATED", updatedEvent);
        
        return mapToEventResponse(updatedEvent);
    }

    @Transactional
    public void deleteEvent(UUID id) {
        Event event = findEventById(id);
        if (event.getStatus() == EventStatus.PUBLISHED) {
            throw new BusinessException("Cannot delete a published event");
        }
        
        eventRepository.delete(event);
        createOutboxMessage("EVENT_DELETED", event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> searchEvents(EventSearchRequest request, Pageable pageable) {
        return eventRepository.searchEvents(
                request.getKeyword(),
                request.getCategoryId(),
                request.getVenueId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getMinPrice(),
                request.getMaxPrice(),
                pageable
        ).map(this::mapToEventResponse);
    }

    @Transactional
    public EventResponse publishEvent(UUID id) {
        Event event = eventRepository.findByIdWithLock(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Event", "id", id));
        
        if (event.isPublished()) {
            throw new BusinessException("Event is already published");
        }
        
        if (event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot publish an event with a past start date");
        }
        
        event.setPublished(true);
        event.setStatus(EventStatus.PUBLISHED);
        
        Event publishedEvent = eventRepository.save(event);
        createOutboxMessage("EVENT_PUBLISHED", publishedEvent);
        
        return mapToEventResponse(publishedEvent);
    }

    private Event findEventById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Event", "id", id));
    }

    @SneakyThrows
    private void createOutboxMessage(String eventType, Event event) {
        OutboxMessage outboxMessage = OutboxMessage.builder()
                .aggregateType("EVENT")  // This determines it will go to "events" topic
                .aggregateId(event.getId().toString())
                .eventType(eventType)    // EVENT_CREATED, EVENT_UPDATED, EVENT_PUBLISHED, EVENT_DELETED
                .payload(objectMapper.writeValueAsString(event))
                .status(OutboxStatus.CREATED)
                .build();
        
        outboxRepository.save(outboxMessage);
        log.info("Created outbox message: {}", outboxMessage);
    }

    private EventResponse mapToEventResponse(Event event) {
        // Map event entity to response DTO
        return new EventResponse();
    }
}
