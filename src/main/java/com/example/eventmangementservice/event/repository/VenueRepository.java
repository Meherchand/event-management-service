package com.example.eventmangementservice.event.repository;

import com.example.eventmangementservice.event.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {
    boolean existsByName(String name);
}
