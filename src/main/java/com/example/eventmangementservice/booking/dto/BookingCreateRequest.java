package com.example.eventmangementservice.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateRequest {
    
    @NotNull(message = "Event ID is required")
    private UUID eventId;
    
    @NotEmpty(message = "At least one ticket type must be selected")
    @Valid
    private List<BookingItemRequest> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingItemRequest {
        
        @NotNull(message = "Ticket type ID is required")
        private UUID ticketTypeId;
        
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}

