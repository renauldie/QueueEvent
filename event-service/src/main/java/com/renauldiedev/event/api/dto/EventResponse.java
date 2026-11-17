package com.renauldiedev.event.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String location;
    private LocalDateTime eventDate;
    private String status;
    private List<TicketTierResponse> ticketTiers;
}
