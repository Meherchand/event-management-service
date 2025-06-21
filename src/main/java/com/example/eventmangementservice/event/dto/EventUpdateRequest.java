package com.example.eventmangementservice.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class EventUpdateRequest {
    
    @Size(min = 3, max = 100, message = "Event name must be between 3 and 100 characters")
    private String name;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;
    
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;
    
    private UUID venueId;
    
    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    private BigDecimal basePrice;
    
    private Set<UUID> categoryIds;
    
    @Valid
    private List<TicketTypeRequest> ticketTypes;
    
    private boolean published;
    
    @Data
    public static class TicketTypeRequest {
        private UUID id;
        
        @NotBlank(message = "Ticket type name is required")
        private String name;
        
        private String description;
        
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        private BigDecimal price;
        
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
