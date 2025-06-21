package com.example.eventmangementservice.booking.repository;

import com.example.eventmangementservice.booking.model.Booking;
import com.example.eventmangementservice.booking.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    
    Optional<Booking> findByBookingNumber(String bookingNumber);
    
    Page<Booking> findByUserId(String userId, Pageable pageable);
    
    List<Booking> findByEventIdAndUserId(UUID eventId, String userId);
    
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime expiryTime);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.event.id = :eventId AND b.status = 'CONFIRMED'")
    long countConfirmedBookingsByEventId(UUID eventId);
}
