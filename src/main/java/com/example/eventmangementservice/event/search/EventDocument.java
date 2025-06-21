package com.example.eventmangementservice.event.search;

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
public class EventDocument {
    
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private VenueDocument venue;
    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal basePrice;
    private EventStatus status;
    private boolean published;
    private List<CategoryDocument> categories;
    private List<TicketTypeDocument> ticketTypes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenueDocument {
        private UUID id;
        private String name;
        private String address;
        private String city;
        private String state;
        private String country;
        private String postalCode;
        private Integer capacity;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDocument {
        private UUID id;
        private String name;
        private String description;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeDocument {
        private UUID id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer quantity;
        private Integer available;
    }
}
