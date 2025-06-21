package com.example.eventmangementservice.event.search;

import com.example.eventmangementservice.event.dto.EventResponse;
import com.example.eventmangementservice.event.dto.EventSearchRequest;
import com.example.eventmangementservice.event.model.Category;
import com.example.eventmangementservice.event.model.Event;
import com.example.eventmangementservice.event.model.TicketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSearchService {

    private final EventSearchRepository eventSearchRepository;

    public Page<EventResponse> searchEvents(EventSearchRequest request, Pageable pageable) {
        Map<String, Object> filters = new HashMap<>();
        
        if (request.getCategoryId() != null) {
            filters.put("categoryId", request.getCategoryId());
        }
        
        if (request.getVenueId() != null) {
            filters.put("venueId", request.getVenueId());
        }
        
        if (request.getStartDate() != null) {
            filters.put("startDate", request.getStartDate());
        }
        
        if (request.getEndDate() != null) {
            filters.put("endDate", request.getEndDate());
        }
        
        if (request.getMinPrice() != null) {
            filters.put("minPrice", request.getMinPrice());
        }
        
        if (request.getMaxPrice() != null) {
            filters.put("maxPrice", request.getMaxPrice());
        }
        
        List<EventDocument> eventDocuments = eventSearchRepository.searchEvents(
                request.getKeyword(),
                filters,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        
        List<EventResponse> eventResponses = eventDocuments.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(eventResponses, pageable, eventResponses.size());
    }

    public void indexEvent(Event event) {
        EventDocument eventDocument = mapToEventDocument(event);
        eventSearchRepository.save(eventDocument);
    }

    public void updateIndexedEvent(Event event) {
        EventDocument eventDocument = mapToEventDocument(event);
        eventSearchRepository.update(eventDocument);
    }

    public void deleteIndexedEvent(UUID eventId) {
        eventSearchRepository.delete(eventId);
    }

    private EventDocument mapToEventDocument(Event event) {
        return EventDocument.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .venue(mapToVenueDocument(event))
                .totalSeats(event.getTotalSeats())
                .availableSeats(event.getAvailableSeats())
                .basePrice(event.getBasePrice())
                .status(event.getStatus())
                .published(event.isPublished())
                .categories(mapToCategoryDocuments(event))
                .ticketTypes(mapToTicketTypeDocuments(event))
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private EventDocument.VenueDocument mapToVenueDocument(Event event) {
        return new EventDocument.VenueDocument(
                event.getVenue().getId(),
                event.getVenue().getName(),
                event.getVenue().getAddress(),
                event.getVenue().getCity(),
                event.getVenue().getState(),
                event.getVenue().getCountry(),
                event.getVenue().getPostalCode(),
                event.getVenue().getCapacity()
        );
    }

    private List<EventDocument.CategoryDocument> mapToCategoryDocuments(Event event) {
        return event.getCategories().stream()
                .map(category -> new EventDocument.CategoryDocument(
                        category.getId(),
                        category.getName(),
                        category.getDescription()
                ))
                .collect(Collectors.toList());
    }

    private List<EventDocument.TicketTypeDocument> mapToTicketTypeDocuments(Event event) {
        return event.getTicketTypes().stream()
                .map(ticketType -> new EventDocument.TicketTypeDocument(
                        ticketType.getId(),
                        ticketType.getName(),
                        ticketType.getDescription(),
                        ticketType.getPrice(),
                        ticketType.getQuantity(),
                        ticketType.getAvailable()
                ))
                .collect(Collectors.toList());
    }

    private EventResponse mapToEventResponse(EventDocument document) {
        return EventResponse.builder()
                .id(document.getId())
                .name(document.getName())
                .description(document.getDescription())
                .startDate(document.getStartDate())
                .endDate(document.getEndDate())
                .venue(mapToVenueDto(document.getVenue()))
                .totalSeats(document.getTotalSeats())
                .availableSeats(document.getAvailableSeats())
                .basePrice(document.getBasePrice())
                .status(document.getStatus())
                .published(document.isPublished())
                .categories(mapToCategoryDtos(document.getCategories()))
                .ticketTypes(mapToTicketTypeDtos(document.getTicketTypes()))
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private EventResponse.VenueDto mapToVenueDto(EventDocument.VenueDocument venueDocument) {
        return new EventResponse.VenueDto(
                venueDocument.getId(),
                venueDocument.getName(),
                venueDocument.getAddress(),
                venueDocument.getCity(),
                venueDocument.getCountry()
        );
    }

    private List<EventResponse.CategoryDto> mapToCategoryDtos(List<EventDocument.CategoryDocument> categoryDocuments) {
        return categoryDocuments.stream()
                .map(categoryDocument -> new EventResponse.CategoryDto(
                        categoryDocument.getId(),
                        categoryDocument.getName()
                ))
                .collect(Collectors.toList());
    }

    private List<EventResponse.TicketTypeDto> mapToTicketTypeDtos(List<EventDocument.TicketTypeDocument> ticketTypeDocuments) {
        return ticketTypeDocuments.stream()
                .map(ticketTypeDocument -> new EventResponse.TicketTypeDto(
                        ticketTypeDocument.getId(),
                        ticketTypeDocument.getName(),
                        ticketTypeDocument.getDescription(),
                        ticketTypeDocument.getPrice(),
                        ticketTypeDocument.getQuantity(),
                        ticketTypeDocument.getAvailable()
                ))
                .collect(Collectors.toList());
    }
}
