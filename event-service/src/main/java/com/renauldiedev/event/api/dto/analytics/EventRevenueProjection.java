package com.renauldiedev.event.api.dto.analytics;

import java.math.BigDecimal;

public interface EventRevenueProjection {
    Long getEventId();
    String getEventTitle();
    String getCategory();
    String getStatus();
    Long getTotalOrders();
    Long getTotalTicketsSold();
    BigDecimal getTotalRevenue();
    Integer getRevenueRank();
    BigDecimal getRevenuePercentage();
}
