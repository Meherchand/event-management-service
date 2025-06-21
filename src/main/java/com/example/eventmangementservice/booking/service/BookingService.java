package com.example.eventmangementservice.booking.service;

import com.example.eventmangementservice.booking.dto.BookingCreateRequest;
import com.example.eventmangementservice.booking.dto.BookingResponse;
import com.example.eventmangementservice.booking.model.Booking;
import com.example.eventmangementservice.booking.model.BookingItem;
import com.example.eventmangementservice.booking.model.BookingStatus;
import com.example.eventmangementservice.booking.repository.BookingItemRepository;
import com.example.eventmangementservice.booking.repository.BookingRepository;
import com.example.eventmangementservice.common.exception.BusinessException;
import com.example.eventmangementservice.common.exception.ResourceNotFoundException;
import com.example.eventmangementservice.common.outbox.OutboxMessage;
import com.example.eventmangementservice.common.outbox.OutboxRepository;
import com.example.eventmangementservice.common.outbox.OutboxStatus;
import com.example.eventmangementservice.event.model.Event;
import com.example.eventmangementservice.event.model.TicketType;
import com.example.eventmangementservice.event.repository.EventRepository;
import com.example.eventmangementservice.event.repository.TicketTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final int BOOKING_EXPIRY_MINUTES = 15;

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(String userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable)
                .map(this::mapToBookingResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID id, String userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", "id", id));
        
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to view this booking");
        }
        
        return mapToBookingResponse(booking);
    }

    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, String userId) {
        // Load the event with a pessimistic lock to prevent race conditions
        Event event = eventRepository.findByIdWithLock(request.getEventId())
                .orElseThrow(() -> ResourceNotFoundException.of("Event", "id", request.getEventId()));
        
        // Validate if event is bookable
        validateEventForBooking(event);
        
        // Process each ticket type and validate availability
        Set<BookingItem> bookingItems = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (BookingCreateRequest.BookingItemRequest itemRequest : request.getItems()) {
            TicketType ticketType = ticketTypeRepository.findByIdWithLock(itemRequest.getTicketTypeId())
                    .orElseThrow(() -> ResourceNotFoundException.of("TicketType", "id", itemRequest.getTicketTypeId()));
            
            // Validate if the ticket type belongs to the requested event
            if (!ticketType.getEvent().getId().equals(event.getId())) {
                throw new BusinessException("Ticket type does not belong to the requested event");
            }
            
            // Check if there are enough tickets available
            if (ticketType.getAvailable() < itemRequest.getQuantity()) {
                throw new BusinessException("Not enough tickets available for " + ticketType.getName());
            }
            
            // Decrease available tickets
            ticketType.setAvailable(ticketType.getAvailable() - itemRequest.getQuantity());
            ticketTypeRepository.save(ticketType);
            
            // Create booking item
            BookingItem bookingItem = new BookingItem();
            bookingItem.setTicketType(ticketType);
            bookingItem.setQuantity(itemRequest.getQuantity());
            bookingItem.setUnitPrice(ticketType.getPrice());
            bookingItem.setTotalPrice(ticketType.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
            
            bookingItems.add(bookingItem);
            totalAmount = totalAmount.add(bookingItem.getTotalPrice());
        }
        
        // Create the booking
        Booking booking = new Booking();
        booking.setBookingNumber(generateBookingNumber());
        booking.setEvent(event);
        booking.setUserId(userId);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(BOOKING_EXPIRY_MINUTES));
        
        Booking savedBooking = bookingRepository.save(booking);
        
        // Link the booking items to the booking
        bookingItems.forEach(item -> item.setBooking(savedBooking));
        savedBooking.setItems(bookingItems);
        
        // Update event available seats
        int totalTickets = bookingItems.stream().mapToInt(BookingItem::getQuantity).sum();
        event.setAvailableSeats(event.getAvailableSeats() - totalTickets);
        eventRepository.save(event);
        
        // Create outbox message
        createOutboxMessage("BOOKING_CREATED", savedBooking);
        
        return mapToBookingResponse(savedBooking);
    }

    @Transactional
    public BookingResponse confirmBooking(UUID id, String userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", "id", id));
        
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to confirm this booking");
        }
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Booking is not in PENDING state");
        }
        
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Booking has expired");
        }
        
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmedBooking = bookingRepository.save(booking);
        
        createOutboxMessage("BOOKING_CONFIRMED", confirmedBooking);
        
        return mapToBookingResponse(confirmedBooking);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID id, String userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", "id", id));
        
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to cancel this booking");
        }
        
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Booking cannot be cancelled in its current state");
        }
        
        // Release the ticket inventory
        for (BookingItem item : booking.getItems()) {
            TicketType ticketType = item.getTicketType();
            ticketType.setAvailable(ticketType.getAvailable() + item.getQuantity());
            ticketTypeRepository.save(ticketType);
        }
        
        // Update event available seats
        Event event = booking.getEvent();
        int totalTickets = booking.getItems().stream().mapToInt(BookingItem::getQuantity).sum();
        event.setAvailableSeats(event.getAvailableSeats() + totalTickets);
        eventRepository.save(event);
        
        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);
        
        createOutboxMessage("BOOKING_CANCELLED", cancelledBooking);
        
        return mapToBookingResponse(cancelledBooking);
    }

    @Transactional
    public void processExpiredBookings() {
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING, LocalDateTime.now());
        
        for (Booking booking : expiredBookings) {
            // Release the ticket inventory
            for (BookingItem item : booking.getItems()) {
                TicketType ticketType = item.getTicketType();
                ticketType.setAvailable(ticketType.getAvailable() + item.getQuantity());
                ticketTypeRepository.save(ticketType);
            }
            
            // Update event available seats
            Event event = booking.getEvent();
            int totalTickets = booking.getItems().stream().mapToInt(BookingItem::getQuantity).sum();
            event.setAvailableSeats(event.getAvailableSeats() + totalTickets);
            eventRepository.save(event);
            
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            
            createOutboxMessage("BOOKING_EXPIRED", booking);
        }
    }

    private void validateEventForBooking(Event event) {
        if (!event.isPublished()) {
            throw new BusinessException("Event is not available for booking");
        }
        
        if (event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Event has already started");
        }
        
        if (event.getAvailableSeats() <= 0) {
            throw new BusinessException("No seats available for this event");
        }
    }

    private String generateBookingNumber() {
        return "BK-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @SneakyThrows
    private void createOutboxMessage(String eventType, Booking booking) {
        OutboxMessage outboxMessage = OutboxMessage.builder()
                .aggregateType("BOOKING")  // This determines it will go to "bookings" topic
                .aggregateId(booking.getId().toString())
                .eventType(eventType)      // BOOKING_CREATED, BOOKING_CONFIRMED, BOOKING_CANCELLED, BOOKING_EXPIRED
                .payload(objectMapper.writeValueAsString(booking))
                .status(OutboxStatus.CREATED)
                .build();
        
        outboxRepository.save(outboxMessage);
        log.info("Created outbox message: {}", outboxMessage);
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .eventId(booking.getEvent().getId())
                .eventName(booking.getEvent().getName())
                .userId(booking.getUserId())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .expiresAt(booking.getExpiresAt())
                .items(booking.getItems().stream()
                        .map(item -> new BookingResponse.BookingItemDto(
                                item.getId(),
                                item.getTicketType().getId(),
                                item.getTicketType().getName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getTotalPrice()
                        ))
                        .collect(Collectors.toList()))
                .createdAt(booking.getCreatedAt())
                .build();
    }
}

