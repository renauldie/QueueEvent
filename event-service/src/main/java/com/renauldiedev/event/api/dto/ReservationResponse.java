package com.renauldiedev.event.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private String reservationCode;
    private Long userId;
    private Long eventId;
    private String eventTitle;
    private Long ticketTierId;
    private String tierName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal estimatedTotal;
    private LocalDateTime expiresAt;
    private String status;
}
