package com.example.eventmangementservice.event.repository;

import com.example.eventmangementservice.event.model.Event;
import com.example.eventmangementservice.event.model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
    Page<Event> findByPublishedTrue(Pageable pageable);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(UUID id);
    
    List<Event> findByStatusAndStartDateBefore(EventStatus status, LocalDateTime dateTime);
    
    @Query("SELECT e FROM Event e WHERE " +
           "(:keyword IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR EXISTS (SELECT c FROM e.categories c WHERE c.id = :categoryId)) AND " +
           "(:venueId IS NULL OR e.venue.id = :venueId) AND " +
           "(:minPrice IS NULL OR e.basePrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR e.basePrice <= :maxPrice) AND " +
           "(:startDate IS NULL OR e.startDate >= :startDate) AND " +
           "(:endDate IS NULL OR e.endDate <= :endDate) AND " +
           "e.published = true")
    Page<Event> searchEvents(
            String keyword, 
            UUID categoryId, 
            UUID venueId, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Double minPrice, 
            Double maxPrice, 
            Pageable pageable);
}
