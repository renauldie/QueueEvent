package com.renauldiedev.event.domain.reservation;

import com.renauldiedev.event.domain.common.BaseEntity;
import com.renauldiedev.event.domain.event.Event;
import com.renauldiedev.event.domain.inventory.TicketTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_res_code", columnList = "reservation_code"),
        @Index(name = "idx_res_user", columnList = "user_id"),
        @Index(name = "idx_res_expires", columnList = "expires_at")
})
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_code", nullable = false, unique = true, length = 64)
    private String reservationCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id", nullable = false)
    private TicketTier ticketTier;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private ReservationStatus status;
}
