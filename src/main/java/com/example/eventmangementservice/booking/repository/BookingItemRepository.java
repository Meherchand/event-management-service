package com.example.eventmangementservice.booking.repository;

import com.example.eventmangementservice.booking.model.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {
    
    List<BookingItem> findByBookingId(UUID bookingId);
    
    @Query("SELECT SUM(bi.quantity) FROM BookingItem bi WHERE bi.ticketType.id = :ticketTypeId AND bi.booking.status = 'CONFIRMED'")
    Integer countSoldTicketsByTicketTypeId(UUID ticketTypeId);
}
