package com.renauldiedev.event.api.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRevenueSummary {
    private String category;
    private BigDecimal totalRevenue;
    private long eventCount;
    private BigDecimal averageRevenue;
}
