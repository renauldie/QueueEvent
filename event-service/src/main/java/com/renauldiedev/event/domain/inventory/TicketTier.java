package com.renauldiedev.event.domain.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.renauldiedev.event.domain.common.BaseEntity;
import com.renauldiedev.event.domain.event.Event;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ticket_tiers", indexes = {
        @Index(name = "idx_tier_event_id", columnList = "event_id")
})
public class TicketTier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private Event event;

    @Column(name = "tier_name", nullable = false, length = 100)
    private String tierName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Column(name = "available_capacity", nullable = false)
    private Integer availableCapacity;

    @Version
    private Long version;
}
