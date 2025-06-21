package com.example.eventmangementservice.event.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EventSearchRepository {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "events";

    @SneakyThrows
    public void save(EventDocument event) {
        IndexResponse response = elasticsearchClient.index(i -> i
                .index(INDEX_NAME)
                .id(event.getId().toString())
                .document(event)
        );
        log.info("Indexed event to Elasticsearch: {}, result: {}", event.getId(), response.result().name());
    }

    @SneakyThrows
    public void update(EventDocument event) {
        UpdateResponse<EventDocument> response = elasticsearchClient.update(
                UpdateRequest.of(u -> u
                        .index(INDEX_NAME)
                        .id(event.getId().toString())
                        .doc(event)
                ),
                EventDocument.class
        );
        log.info("Updated event in Elasticsearch: {}, result: {}", event.getId(), response.result().name());
    }

    @SneakyThrows
    public void delete(UUID id) {
        DeleteResponse response = elasticsearchClient.delete(d -> d
                .index(INDEX_NAME)
                .id(id.toString())
        );
        log.info("Deleted event from Elasticsearch: {}, result: {}", id, response.result().name());
    }

    @SneakyThrows
    public EventDocument findById(UUID id) {
        GetResponse<EventDocument> response = elasticsearchClient.get(g -> g
                .index(INDEX_NAME)
                .id(id.toString()),
                EventDocument.class
        );
        
        if (response.found()) {
            return response.source();
        }
        return null;
    }

    @SneakyThrows
    public List<EventDocument> searchEvents(String keyword, Map<String, Object> filters, int page, int size) {
        SearchResponse<EventDocument> response = elasticsearchClient.search(s -> {
            s.index(INDEX_NAME);
            
            // Add keyword search
            if (keyword != null && !keyword.trim().isEmpty()) {
                s.query(q -> q
                        .multiMatch(m -> m
                                .fields("name", "description", "venue.name", "venue.address", "categories.name")
                                .query(keyword)
                                .fuzziness("AUTO")
                        )
                );
            }
            
            // Add filters
            if (filters != null && !filters.isEmpty()) {
                // Apply filters logic here
                // For example: filter by category, venue, price range, etc.
            }
            
            // Add pagination
            s.from(page * size);
            s.size(size);
            
            return s;
        }, EventDocument.class);
        
        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }
}
