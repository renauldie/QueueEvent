package com.renauldiedev.event.domain.analytics;

import com.renauldiedev.event.api.dto.analytics.CategoryRevenueSummary;
import com.renauldiedev.event.api.dto.analytics.EventRevenueProjection;
import com.renauldiedev.event.api.dto.analytics.PlatformAnalyticsResponse;
import com.renauldiedev.event.api.dto.analytics.ReservationTrendProjection;
import com.renauldiedev.event.api.dto.analytics.TierOccupancyProjection;

import com.renauldiedev.event.domain.analytics.AnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Transactional(readOnly = true)
    public PlatformAnalyticsResponse getPlatformAnalytics() {

        List<EventRevenueProjection> revenueProjections = analyticsRepository.getEventRevenueRankings();
        List<TierOccupancyProjection> occupancyProjections = analyticsRepository.getTierOccupancyRankings();
        List<ReservationTrendProjection> trendProjections = analyticsRepository.getDailyReservationTrends();

        BigDecimal totalPlatformRevenue = revenueProjections.stream()
                .map(p -> p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTicketsSold = revenueProjections.stream()
                .mapToLong(p -> p.getTotalTicketsSold() != null ? p.getTotalTicketsSold() : 0L)
                .sum();

        List<EventRevenueProjection> activeRevenueEvents = revenueProjections.stream()
                .filter(p -> p.getTotalRevenue() != null && p.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        Map<String, List<EventRevenueProjection>> eventsByCategory = revenueProjections.stream()
                .collect(Collectors.groupingBy(EventRevenueProjection::getCategory));

        List<CategoryRevenueSummary> categoryBreakdowns = eventsByCategory.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<EventRevenueProjection> categoryEvents = entry.getValue();

                    BigDecimal categoryRevenue = categoryEvents.stream()
                            .map(p -> p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long count = categoryEvents.size();
                    BigDecimal averageRevenue = count > 0
                            ? categoryRevenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return CategoryRevenueSummary.builder()
                            .category(category)
                            .totalRevenue(categoryRevenue)
                            .eventCount(count)
                            .averageRevenue(averageRevenue)
                            .build();
                })
                .sorted((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()))
                .toList();

        return PlatformAnalyticsResponse.builder()
                .totalPlatformRevenue(totalPlatformRevenue)
                .totalTicketsSold(totalTicketsSold)
                .totalEvents(revenueProjections.size())
                .topRevenueEvents(revenueProjections)
                .categoryBreakdown(categoryBreakdowns)
                .tierOccupancyRankings(occupancyProjections)
                .reservationTrends(trendProjections)
                .build();
    }
}
