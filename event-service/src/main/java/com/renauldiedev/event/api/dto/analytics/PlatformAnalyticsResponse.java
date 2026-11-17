package com.renauldiedev.event.api.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAnalyticsResponse {
    private BigDecimal totalPlatformRevenue;
    private long totalTicketsSold;
    private long totalEvents;
    private List<EventRevenueProjection> topRevenueEvents;
    private List<CategoryRevenueSummary> categoryBreakdown;
    private List<TierOccupancyProjection> tierOccupancyRankings;
    private List<ReservationTrendProjection> reservationTrends;
}
