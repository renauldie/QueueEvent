package com.renauldiedev.event.api.controller;

import com.renauldiedev.event.domain.event.Event;

import com.renauldiedev.event.api.dto.ApiResponse;
import com.renauldiedev.event.api.dto.analytics.EventRevenueProjection;
import com.renauldiedev.event.api.dto.analytics.PlatformAnalyticsResponse;
import com.renauldiedev.event.api.dto.analytics.ReservationTrendProjection;
import com.renauldiedev.event.api.dto.analytics.TierOccupancyProjection;
import com.renauldiedev.event.domain.analytics.AnalyticsRepository;
import com.renauldiedev.event.domain.analytics.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics & Reporting", description = "Endpoints powered by Advanced Native SQL (CTE, Window Functions, Ranking) & Java Stream")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsRepository analyticsRepository;

    public AnalyticsController(AnalyticsService analyticsService, AnalyticsRepository analyticsRepository) {
        this.analyticsService = analyticsService;
        this.analyticsRepository = analyticsRepository;
    }

    @GetMapping("/platform")
    @Operation(summary = "Get full platform analytics combining Native SQL CTEs, Window Functions, and Java Stream aggregations")
    public ResponseEntity<ApiResponse<PlatformAnalyticsResponse>> getPlatformAnalytics() {
        PlatformAnalyticsResponse analytics = analyticsService.getPlatformAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/revenue-rankings")
    @Operation(summary = "Event revenue rankings using CTE and DENSE_RANK() window function")
    public ResponseEntity<ApiResponse<List<EventRevenueProjection>>> getEventRevenueRankings() {
        List<EventRevenueProjection> rankings = analyticsRepository.getEventRevenueRankings();
        return ResponseEntity.ok(ApiResponse.success(rankings));
    }

    @GetMapping("/tier-occupancy")
    @Operation(summary = "Tier occupancy and demand rankings using CTE and ROW_NUMBER() PARTITION BY window function")
    public ResponseEntity<ApiResponse<List<TierOccupancyProjection>>> getTierOccupancyRankings() {
        List<TierOccupancyProjection> occupancy = analyticsRepository.getTierOccupancyRankings();
        return ResponseEntity.ok(ApiResponse.success(occupancy));
    }

    @GetMapping("/reservation-trends")
    @Operation(summary = "Daily reservation trends using CTE, cumulative SUM() OVER(), and LAG() window function")
    public ResponseEntity<ApiResponse<List<ReservationTrendProjection>>> getReservationTrends() {
        List<ReservationTrendProjection> trends = analyticsRepository.getDailyReservationTrends();
        return ResponseEntity.ok(ApiResponse.success(trends));
    }
}
