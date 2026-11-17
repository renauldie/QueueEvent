package com.renauldiedev.event.infrastructure.elasticsearch.service;

import com.renauldiedev.event.domain.event.Event;
import com.renauldiedev.event.domain.inventory.TicketTier;
import com.renauldiedev.event.infrastructure.elasticsearch.document.EventDocument;
import com.renauldiedev.event.infrastructure.elasticsearch.repository.EventSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventSearchService {

    private final EventSearchRepository eventSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public EventSearchService(
            EventSearchRepository eventSearchRepository,
            ElasticsearchOperations elasticsearchOperations) {
        this.eventSearchRepository = eventSearchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public Page<EventDocument> searchEvents(
            String keyword,
            String category,
            String location,
            String status,
            Double minPrice,
            Double maxPrice,
            Pageable pageable) {

        Criteria criteria = new Criteria();

        if (StringUtils.hasText(keyword)) {
            Criteria searchCrit = new Criteria("title").contains(keyword)
                    .or(new Criteria("description").contains(keyword))
                    .or(new Criteria("location").contains(keyword));
            criteria = criteria.and(searchCrit);
        }

        if (StringUtils.hasText(category)) {
            criteria = criteria.and(new Criteria("category").is(category));
        }

        if (StringUtils.hasText(location)) {
            criteria = criteria.and(new Criteria("location").contains(location));
        }

        if (StringUtils.hasText(status)) {
            criteria = criteria.and(new Criteria("status").is(status));
        }

        if (minPrice != null) {
            criteria = criteria.and(new Criteria("minPrice").greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("maxPrice").lessThanEqual(maxPrice));
        }

        CriteriaQuery query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<EventDocument> searchHits = elasticsearchOperations.search(query, EventDocument.class);

        List<EventDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    public EventDocument indexEvent(Event event) {
        try {
            EventDocument document = mapToDocument(event);
            EventDocument saved = eventSearchRepository.save(document);
            log.info("Successfully indexed event [{}] - '{}' to Elasticsearch.", saved.getId(), saved.getTitle());
            return saved;
        } catch (Exception e) {
            log.warn("Failed to index event [{}] to Elasticsearch: {}. Continuing without blocking DB transaction.",
                    event.getId(), e.getMessage());
            return null;
        }
    }

    public void deleteEventFromIndex(Long eventId) {
        try {
            eventSearchRepository.deleteById(String.valueOf(eventId));
            log.info("Deleted event [{}] from Elasticsearch index.", eventId);
        } catch (Exception e) {
            log.warn("Failed to delete event [{}] from Elasticsearch: {}", eventId, e.getMessage());
        }
    }

    public void reindexAll(List<Event> events) {
        try {
            List<EventDocument> documents = events.stream()
                    .map(this::mapToDocument)
                    .collect(Collectors.toList());
            eventSearchRepository.saveAll(documents);
            log.info("Successfully re-indexed {} events into Elasticsearch.", documents.size());
        } catch (Exception e) {
            log.warn("Failed to bulk reindex events to Elasticsearch: {}", e.getMessage());
        }
    }

    private EventDocument mapToDocument(Event event) {
        List<TicketTier> tiers = event.getTicketTiers() != null ? event.getTicketTiers() : List.of();

        double minPrice = tiers.stream()
                .map(t -> t.getPrice().doubleValue())
                .min(Double::compareTo)
                .orElse(0.0);

        double maxPrice = tiers.stream()
                .map(t -> t.getPrice().doubleValue())
                .max(Double::compareTo)
                .orElse(0.0);

        int totalCap = tiers.stream()
                .mapToInt(TicketTier::getTotalCapacity)
                .sum();

        int availableCap = tiers.stream()
                .mapToInt(TicketTier::getAvailableCapacity)
                .sum();

        return EventDocument.builder()
                .id(String.valueOf(event.getId()))
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .status(event.getStatus() != null ? event.getStatus().getName() : "DRAFT")
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalCapacity(totalCap)
                .availableCapacity(availableCap)
                .build();
    }
}
