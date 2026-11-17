package com.renauldiedev.event.domain.reservation;

import com.renauldiedev.event.domain.event.EventRepository;
import com.renauldiedev.event.domain.inventory.SeatInventoryRepository;
import com.renauldiedev.event.domain.inventory.SeatStatusRepository;
import com.renauldiedev.event.domain.inventory.TicketTierRepository;

import com.renauldiedev.event.domain.event.Event;
import com.renauldiedev.event.domain.inventory.SeatInventory;
import com.renauldiedev.event.domain.inventory.SeatStatus;
import com.renauldiedev.event.domain.inventory.TicketTier;
import com.renauldiedev.event.domain.reservation.Reservation;
import com.renauldiedev.event.domain.reservation.ReservationStatus;
import com.renauldiedev.event.exception.BadRequestException;
import com.renauldiedev.event.exception.ResourceNotFoundException;
import com.renauldiedev.event.exception.SeatUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ReservationTransactionHandler {

    private static final int RESERVATION_HOLD_MINUTES = 10;
    private static final String REDIS_HOLD_PREFIX = "reservation:hold:";

    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final SeatStatusRepository seatStatusRepository;
    private final ReservationStatusRepository reservationStatusRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public ReservationTransactionHandler(
            EventRepository eventRepository,
            TicketTierRepository ticketTierRepository,
            SeatInventoryRepository seatInventoryRepository,
            SeatStatusRepository seatStatusRepository,
            ReservationStatusRepository reservationStatusRepository,
            ReservationRepository reservationRepository,
            RedisTemplate<String, Object> redisTemplate) {
        this.eventRepository = eventRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.seatStatusRepository = seatStatusRepository;
        this.reservationStatusRepository = reservationStatusRepository;
        this.reservationRepository = reservationRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Reservation executeReservationInTransaction(Long userId, Long eventId, Long ticketTierId, Integer quantity) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (!"PUBLISHED".equalsIgnoreCase(event.getStatus().getName())) {
            throw new BadRequestException("Event is not currently open for reservations");
        }

        TicketTier tier = ticketTierRepository.findByIdWithLock(ticketTierId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket tier not found with id: " + ticketTierId));

        if (!tier.getEvent().getId().equals(eventId)) {
            throw new BadRequestException("Ticket tier does not belong to the specified event");
        }

        if (tier.getAvailableCapacity() < quantity) {
            throw new SeatUnavailableException("Not enough tickets available. Remaining capacity: " + tier.getAvailableCapacity());
        }

        tier.setAvailableCapacity(tier.getAvailableCapacity() - quantity);
        ticketTierRepository.save(tier);

        SeatStatus heldStatus = seatStatusRepository.findByName("HELD")
                .orElseThrow(() -> new ResourceNotFoundException("Status HELD not found in database"));
        List<SeatInventory> seatsToHold = seatInventoryRepository.findAvailableSeatsForReservation(tier.getId(), PageRequest.of(0, quantity));
        for (SeatInventory seat : seatsToHold) {
            seat.setStatus(heldStatus);
            seatInventoryRepository.save(seat);
        }

        ReservationStatus pendingStatus = reservationStatusRepository.findByName("PENDING")
                .orElseThrow(() -> new ResourceNotFoundException("Status PENDING not found in database"));

        String reservationCode = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESERVATION_HOLD_MINUTES);

        Reservation reservation = Reservation.builder()
                .reservationCode(reservationCode)
                .userId(userId)
                .event(event)
                .ticketTier(tier)
                .quantity(quantity)
                .expiresAt(expiresAt)
                .status(pendingStatus)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        String holdKey = REDIS_HOLD_PREFIX + reservationCode;
        redisTemplate.opsForValue().set(holdKey, userId, Duration.ofMinutes(RESERVATION_HOLD_MINUTES));

        log.info("Created temporary reservation [{}] for user [{}] with {} tickets, expires at {}. Redis hold key set.",
                reservationCode, userId, quantity, expiresAt);

        return savedReservation;
    }
}
