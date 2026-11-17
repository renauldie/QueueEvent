package com.renauldiedev.event.api.controller;

import com.renauldiedev.event.domain.event.Event;
import com.renauldiedev.event.infrastructure.elasticsearch.document.EventDocument;
import com.renauldiedev.event.domain.event.EventRepository;
import com.renauldiedev.event.infrastructure.elasticsearch.service.EventSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events/search")
@Tag(name = "Event Search", description = "Elasticsearch full-text search, filtering, and indexing")
public class EventSearchController {

    private final EventSearchService eventSearchService;
    private final EventRepository eventRepository;

    public EventSearchController(
            EventSearchService eventSearchService,
            EventRepository eventRepository) {
        this.eventSearchService = eventSearchService;
        this.eventRepository = eventRepository;
    }

    @GetMapping
    @Operation(summary = "Search events using full-text query, category, location, and price range filters")
    public ResponseEntity<Page<EventDocument>> searchEvents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, defaultValue = "PUBLISHED") String status,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EventDocument> results = eventSearchService.searchEvents(
                q, category, location, status, minPrice, maxPrice, pageable);

        return ResponseEntity.ok(results);
    }

    @PostMapping("/reindex")
    @Operation(summary = "Reindex all database events into Elasticsearch")
    public ResponseEntity<Map<String, Object>> reindexAll() {
        List<Event> allEvents = eventRepository.findAll();
        eventSearchService.reindexAll(allEvents);
        return ResponseEntity.ok(Map.of(
                "message", "Reindexing triggered successfully",
                "indexedCount", allEvents.size()
        ));
    }
}
