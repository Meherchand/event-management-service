package com.example.eventmangementservice.event.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(indexName = "events")
public class EventDocument {
    @Id
    private String id;
    
    @Field(type = FieldType.Text, name = "name")
    private String name;
    
    @Field(type = FieldType.Text, name = "description")
    private String description;
    
    @Field(type = FieldType.Date, name = "startDate")
    private LocalDateTime startDate;
    
    @Field(type = FieldType.Date, name = "endDate")
    private LocalDateTime endDate;
    
    @Field(type = FieldType.Text, name = "venue")
    private String venue;
    
    @Field(type = FieldType.Integer, name = "availableSeats")
    private Integer availableSeats;
    
    @Field(type = FieldType.Double, name = "ticketPrice")
    private BigDecimal ticketPrice;
    
    @Field(type = FieldType.Keyword, name = "category")
    private String category;
    
    @Field(type = FieldType.Boolean, name = "active")
    private boolean active;
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public String getVenue() {
        return venue;
    }
    
    public void setVenue(String venue) {
        this.venue = venue;
    }
    
    public Integer getAvailableSeats() {
        return availableSeats;
    }
    
    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }
    
    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }
    
    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
}
