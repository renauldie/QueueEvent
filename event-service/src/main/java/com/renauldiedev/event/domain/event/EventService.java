package com.renauldiedev.event.domain.event;

import com.renauldiedev.event.api.dto.EventResponse;
import com.renauldiedev.event.api.dto.TicketTierResponse;

import com.renauldiedev.event.domain.event.Event;
import com.renauldiedev.event.domain.event.EventStatus;
import com.renauldiedev.event.domain.inventory.TicketTier;
import com.renauldiedev.event.exception.ResourceNotFoundException;
import com.renauldiedev.event.domain.event.EventRepository;
import com.renauldiedev.event.domain.event.EventStatusRepository;
import com.renauldiedev.event.infrastructure.elasticsearch.service.EventSearchService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventStatusRepository eventStatusRepository;
    private final EventSearchService eventSearchService;

    public EventService(
            EventRepository eventRepository,
            EventStatusRepository eventStatusRepository,
            EventSearchService eventSearchService) {
        this.eventRepository = eventRepository;
        this.eventStatusRepository = eventStatusRepository;
        this.eventSearchService = eventSearchService;
    }

    @Transactional
    public Event createEvent(String title, String description, String category, String location, LocalDateTime eventDate, List<TicketTier> tiers) {
        EventStatus draftStatus = eventStatusRepository.findByName("DRAFT")
                .orElseThrow(() -> new ResourceNotFoundException("Status DRAFT not found in database"));

        Event event = Event.builder()
                .title(title)
                .description(description)
                .category(category)
                .location(location)
                .eventDate(eventDate)
                .status(draftStatus)
                .build();

        if (tiers != null) {
            tiers.forEach(event::addTicketTier);
        }

        Event saved = eventRepository.save(event);
        eventSearchService.indexEvent(saved);
        return saved;
    }

    @CacheEvict(value = "events", key = "#eventId")
    @Transactional
    public Event publishEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        EventStatus publishedStatus = eventStatusRepository.findByName("PUBLISHED")
                .orElseThrow(() -> new ResourceNotFoundException("Status PUBLISHED not found in database"));

        event.setStatus(publishedStatus);
        Event saved = eventRepository.save(event);
        eventSearchService.indexEvent(saved);
        return saved;
    }

    @Cacheable(value = "events", key = "#eventId", unless = "#result == null")
    @Transactional(readOnly = true)
    public com.renauldiedev.event.api.dto.EventResponse getEventDetail(Long eventId) {
        Event event = eventRepository.findByIdWithTiers(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        return mapToEventResponse(event);
    }

    public com.renauldiedev.event.api.dto.EventResponse mapToEventResponse(Event event) {
        List<com.renauldiedev.event.api.dto.TicketTierResponse> tierResponses = event.getTicketTiers() != null
                ? event.getTicketTiers().stream()
                .map(t -> com.renauldiedev.event.api.dto.TicketTierResponse.builder()
                        .id(t.getId())
                        .tierName(t.getTierName())
                        .price(t.getPrice())
                        .totalCapacity(t.getTotalCapacity())
                        .availableCapacity(t.getAvailableCapacity())
                        .build())
                .toList()
                : List.of();

        return com.renauldiedev.event.api.dto.EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .status(event.getStatus() != null ? event.getStatus().getName() : null)
                .ticketTiers(tierResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<Event> listPublishedEvents(Pageable pageable) {
        return eventRepository.findByStatusName("PUBLISHED", pageable);
    }

    @Transactional(readOnly = true)
    public Page<Event> searchByCategory(String category, Pageable pageable) {
        return eventRepository.findByCategoryIgnoreCase(category, pageable);
    }
}
