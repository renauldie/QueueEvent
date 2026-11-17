package com.renauldiedev.event.domain.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.renauldiedev.event.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "seat_inventories", indexes = {
        @Index(name = "idx_seat_number", columnList = "seat_number")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_tier_seat_number", columnNames = {"ticket_tier_id", "seat_number"})
})
public class SeatInventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id", nullable = false)
    @JsonIgnore
    private TicketTier ticketTier;

    @Column(name = "seat_number", nullable = false, length = 50)
    private String seatNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private SeatStatus status;

    @Version
    private Long version;
}
