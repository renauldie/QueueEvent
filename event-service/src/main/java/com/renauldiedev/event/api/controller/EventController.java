package com.renauldiedev.event.api.controller;

import com.renauldiedev.event.api.dto.ApiResponse;
import com.renauldiedev.event.api.dto.CreateEventRequest;
import com.renauldiedev.event.api.dto.EventResponse;
import com.renauldiedev.event.api.dto.TicketTierResponse;

import com.renauldiedev.event.domain.event.Event;
import com.renauldiedev.event.domain.inventory.TicketTier;
import com.renauldiedev.event.domain.event.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Management", description = "Endpoints for creating, publishing, and browsing events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Create a new event with ticket tiers (Admin)")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody CreateEventRequest request) {
        List<TicketTier> tiers = request.getTicketTiers().stream()
                .map(tierReq -> TicketTier.builder()
                        .tierName(tierReq.getTierName())
                        .price(tierReq.getPrice())
                        .totalCapacity(tierReq.getTotalCapacity())
                        .availableCapacity(tierReq.getTotalCapacity())
                        .build())
                .toList();

        Event createdEvent = eventService.createEvent(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getLocation(),
                request.getEventDate(),
                tiers
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created successfully in DRAFT status", mapToEventResponse(createdEvent)));
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish an event to make it available for booking")
    public ResponseEntity<ApiResponse<EventResponse>> publishEvent(@PathVariable("id") Long id) {
        Event publishedEvent = eventService.publishEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Event published successfully", mapToEventResponse(publishedEvent)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information about an event including ticket tier availability")
    public ResponseEntity<ApiResponse<EventResponse>> getEventDetail(@PathVariable("id") Long id) {
        EventResponse event = eventService.getEventDetail(id);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @GetMapping
    @Operation(summary = "List published events with pagination")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Page<Event> eventPage = eventService.listPublishedEvents(PageRequest.of(page, size, sort));
        Page<EventResponse> responsePage = eventPage.map(this::mapToEventResponse);

        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    private EventResponse mapToEventResponse(Event event) {
        List<TicketTierResponse> tierResponses = event.getTicketTiers() != null
                ? event.getTicketTiers().stream()
                .map(t -> TicketTierResponse.builder()
                        .id(t.getId())
                        .tierName(t.getTierName())
                        .price(t.getPrice())
                        .totalCapacity(t.getTotalCapacity())
                        .availableCapacity(t.getAvailableCapacity())
                        .build())
                .toList()
                : List.of();

        return EventResponse.builder()
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
}
