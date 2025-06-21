package com.example.eventmangementservice.event.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventSearchRequest {
    private String keyword;
    private UUID categoryId;
    private UUID venueId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double minPrice;
    private Double maxPrice;
}
