package com.example.eventmangementservice.event.dto;

import com.example.eventmangementservice.event.model.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private VenueDto venue;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal basePrice;
    private EventStatus status;
    private boolean published;
    private List<CategoryDto> categories;
    private List<TicketTypeDto> ticketTypes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenueDto {
        private UUID id;
        private String name;
        private String address;
        private String city;
        private String country;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private UUID id;
        private String name;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeDto {
        private UUID id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer quantity;
        private Integer available;
    }
}
