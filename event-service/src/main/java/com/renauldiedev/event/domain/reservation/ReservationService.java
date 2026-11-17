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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReservationService {

    private static final int RESERVATION_HOLD_MINUTES = 10;
    private static final String REDIS_HOLD_PREFIX = "reservation:hold:";
    private static final String REDIS_LOCK_PREFIX = "lock:tier:";

    private final ReservationRepository reservationRepository;
    private final ReservationStatusRepository reservationStatusRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final SeatStatusRepository seatStatusRepository;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ReservationTransactionHandler reservationTransactionHandler;

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationStatusRepository reservationStatusRepository,
            EventRepository eventRepository,
            TicketTierRepository ticketTierRepository,
            SeatInventoryRepository seatInventoryRepository,
            SeatStatusRepository seatStatusRepository,
            RedissonClient redissonClient,
            RedisTemplate<String, Object> redisTemplate,
            ReservationTransactionHandler reservationTransactionHandler) {
        this.reservationRepository = reservationRepository;
        this.reservationStatusRepository = reservationStatusRepository;
        this.eventRepository = eventRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.seatStatusRepository = seatStatusRepository;
        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
        this.reservationTransactionHandler = reservationTransactionHandler;
    }

    public Reservation createReservation(Long userId, Long eventId, Long ticketTierId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        RLock lock = redissonClient.getLock(REDIS_LOCK_PREFIX + ticketTierId);
        boolean acquired;
        try {
            acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SeatUnavailableException("Thread was interrupted while waiting for booking lock");
        }

        if (!acquired) {
            throw new SeatUnavailableException("High booking traffic for this ticket tier. Please retry in a moment.");
        }

        try {
            return reservationTransactionHandler.executeReservationInTransaction(userId, eventId, ticketTierId, quantity);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(String reservationCode) {
        return reservationRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with code: " + reservationCode));
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredList = reservationRepository.findExpiredPendingReservations(now);

        if (expiredList.isEmpty()) {
            return;
        }

        log.info("Found {} expired reservations to release back into inventory...", expiredList.size());

        ReservationStatus expiredStatus = reservationStatusRepository.findByName("EXPIRED")
                .orElseThrow(() -> new ResourceNotFoundException("Status EXPIRED not found in database"));
        SeatStatus availableStatus = seatStatusRepository.findByName("AVAILABLE")
                .orElseThrow(() -> new ResourceNotFoundException("Status AVAILABLE not found in database"));

        for (Reservation res : expiredList) {
            res.setStatus(expiredStatus);

            TicketTier tier = res.getTicketTier();
            tier.setAvailableCapacity(tier.getAvailableCapacity() + res.getQuantity());
            ticketTierRepository.save(tier);

            List<SeatInventory> heldSeats = seatInventoryRepository.findByTicketTierIdAndStatusName(tier.getId(), "HELD");
            int released = 0;
            for (SeatInventory seat : heldSeats) {
                if (released < res.getQuantity()) {
                    seat.setStatus(availableStatus);
                    seatInventoryRepository.save(seat);
                    released++;
                }
            }

            reservationRepository.save(res);

            redisTemplate.delete(REDIS_HOLD_PREFIX + res.getReservationCode());

            log.info("Released expired reservation [{}] and restored {} seats.", res.getReservationCode(), res.getQuantity());
        }
    }
}
