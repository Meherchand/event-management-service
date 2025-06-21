package com.example.eventmangementservice.event.service;

import com.example.eventmangementservice.event.model.EventDocument;
import com.example.eventmangementservice.event.repository.EventSearchRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventSearchService {
    private final EventSearchRepository eventSearchRepository;
    
    public EventSearchService(EventSearchRepository eventSearchRepository) {
        this.eventSearchRepository = eventSearchRepository;
    }
    
    public List<EventDocument> searchEventsByName(String name) {
        return eventSearchRepository.findByNameContainingAndActiveTrueOrderByStartDateAsc(name);
    }
    
    public List<EventDocument> searchEventsByCategory(String category) {
        return eventSearchRepository.findByCategoryAndStartDateAfterAndActiveTrueOrderByStartDateAsc(
                category, LocalDateTime.now());
    }
    
    public void indexEvent(EventDocument eventDocument) {
        eventSearchRepository.save(eventDocument);
    }
    
    public void deleteEventIndex(String id) {
        eventSearchRepository.deleteById(id);
    }
}
