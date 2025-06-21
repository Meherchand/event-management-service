package com.example.eventmangementservice.event.controller;

import com.example.eventmangementservice.common.dto.ApiResponse;
import com.example.eventmangementservice.event.dto.EventCreateRequest;
import com.example.eventmangementservice.event.dto.EventResponse;
import com.example.eventmangementservice.event.dto.EventSearchRequest;
import com.example.eventmangementservice.event.dto.EventUpdateRequest;
import com.example.eventmangementservice.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getAllEvents(Pageable pageable) {
        Page<EventResponse> events = eventService.getAllEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable UUID id) {
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventResponse event = eventService.createEvent(request);
        return new ResponseEntity<>(ApiResponse.success("Event created successfully", event), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventUpdateRequest request) {
        EventResponse event = eventService.updateEvent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", event));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully", null));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> publishEvent(@PathVariable UUID id) {
        EventResponse event = eventService.publishEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Event published successfully", event));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> searchEvents(
            @ModelAttribute EventSearchRequest request,
            Pageable pageable) {
        Page<EventResponse> events = eventService.searchEvents(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
