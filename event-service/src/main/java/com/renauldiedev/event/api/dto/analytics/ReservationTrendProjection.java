package com.renauldiedev.event.api.dto.analytics;

public interface ReservationTrendProjection {
    String getReservationDate();
    Long getDailyCount();
    Long getCumulativeTotal();
    Long getPreviousDayCount();
}
