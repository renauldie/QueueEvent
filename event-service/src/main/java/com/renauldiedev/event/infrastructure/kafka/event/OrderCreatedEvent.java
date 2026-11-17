package com.renauldiedev.event.infrastructure.kafka.event;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private String orderNumber;
    private Long userId;
    private Long eventId;
    private Long ticketTierId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String createdAt;
}
