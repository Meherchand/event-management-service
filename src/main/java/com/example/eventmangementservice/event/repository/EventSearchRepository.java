package com.example.eventmangementservice.event.repository;

import com.example.eventmangementservice.event.model.EventDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventSearchRepository extends ElasticsearchRepository<EventDocument, String> {
    List<EventDocument> findByNameContainingAndActiveTrueOrderByStartDateAsc(String name);
    List<EventDocument> findByCategoryAndStartDateAfterAndActiveTrueOrderByStartDateAsc(String category, LocalDateTime date);
}
