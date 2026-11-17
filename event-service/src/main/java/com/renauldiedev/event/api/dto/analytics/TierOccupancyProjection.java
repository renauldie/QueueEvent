package com.renauldiedev.event.api.dto.analytics;

import java.math.BigDecimal;

public interface TierOccupancyProjection {
    Long getEventId();
    String getEventTitle();
    Long getTierId();
    String getTierName();
    BigDecimal getPrice();
    Integer getTotalCapacity();
    Integer getAvailableCapacity();
    Long getSoldTickets();
    Integer getTierRankInEvent();
    BigDecimal getOccupancyRate();
}
