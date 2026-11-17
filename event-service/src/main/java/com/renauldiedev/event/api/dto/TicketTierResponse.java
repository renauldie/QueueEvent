package com.renauldiedev.event.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTierResponse {

    private Long id;
    private String tierName;
    private BigDecimal price;
    private Integer totalCapacity;
    private Integer availableCapacity;
}
