package com.renauldiedev.event.domain.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renauldiedev.event.domain.inventory.SeatInventory;
import com.renauldiedev.event.domain.inventory.SeatStatus;
import com.renauldiedev.event.domain.outbox.OutboxEvent;
import com.renauldiedev.event.domain.outbox.OutboxStatus;
import com.renauldiedev.event.domain.reservation.Reservation;
import com.renauldiedev.event.domain.reservation.ReservationStatus;
import com.renauldiedev.event.domain.inventory.SeatInventoryRepository;
import com.renauldiedev.event.domain.inventory.SeatStatusRepository;
import com.renauldiedev.event.domain.reservation.ReservationRepository;
import com.renauldiedev.event.domain.reservation.ReservationStatusRepository;
import com.renauldiedev.event.domain.outbox.OutboxEventRepository;
import com.renauldiedev.event.domain.outbox.OutboxStatusRepository;
import com.renauldiedev.event.infrastructure.redis.IdempotencyService;
import com.renauldiedev.event.exception.BadRequestException;
import com.renauldiedev.event.exception.ReservationExpiredException;
import com.renauldiedev.event.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationStatusRepository reservationStatusRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final SeatStatusRepository seatStatusRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxStatusRepository outboxStatusRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OrderStatusRepository orderStatusRepository,
            ReservationRepository reservationRepository,
            ReservationStatusRepository reservationStatusRepository,
            SeatInventoryRepository seatInventoryRepository,
            SeatStatusRepository seatStatusRepository,
            OutboxEventRepository outboxEventRepository,
            OutboxStatusRepository outboxStatusRepository,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.reservationRepository = reservationRepository;
        this.reservationStatusRepository = reservationStatusRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.seatStatusRepository = seatStatusRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxStatusRepository = outboxStatusRepository;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrderFromReservation(String reservationCode, Long userId) {

        idempotencyService.validateUniqueRequest("order:" + reservationCode, java.time.Duration.ofMinutes(15));

        Reservation reservation = reservationRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with code: " + reservationCode));

        if (!reservation.getUserId().equals(userId)) {
            throw new BadRequestException("Reservation does not belong to the authenticated user");
        }

        if (!"PENDING".equalsIgnoreCase(reservation.getStatus().getName())) {
            throw new BadRequestException("Reservation is not in PENDING status");
        }

        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ReservationExpiredException("Reservation hold has expired");
        }

        BigDecimal unitPrice = reservation.getTicketTier().getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(reservation.getQuantity()));

        OrderStatus pendingPaymentStatus = orderStatusRepository.findByName("PENDING_PAYMENT")
                .orElseThrow(() -> new ResourceNotFoundException("Status PENDING_PAYMENT not found in database"));

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .reservationId(reservation.getId())
                .event(reservation.getEvent())
                .ticketTier(reservation.getTicketTier())
                .quantity(reservation.getQuantity())
                .totalAmount(totalAmount)
                .status(pendingPaymentStatus)
                .build();

        Order savedOrder = orderRepository.save(order);

        ReservationStatus confirmedStatus = reservationStatusRepository.findByName("CONFIRMED")
                .orElseThrow(() -> new ResourceNotFoundException("Status CONFIRMED not found in database"));
        reservation.setStatus(confirmedStatus);
        reservationRepository.save(reservation);

        SeatStatus reservedSeatStatus = seatStatusRepository.findByName("RESERVED")
                .orElseThrow(() -> new ResourceNotFoundException("Status RESERVED not found in database"));
        List<SeatInventory> heldSeats = seatInventoryRepository.findByTicketTierIdAndStatusName(reservation.getTicketTier().getId(), "HELD");
        int transitioned = 0;
        for (SeatInventory seat : heldSeats) {
            if (transitioned < reservation.getQuantity()) {
                seat.setStatus(reservedSeatStatus);
                seatInventoryRepository.save(seat);
                transitioned++;
            }
        }

        saveOutboxEvent("ORDER", savedOrder.getOrderNumber(), "ORDER_CREATED", Map.of(
                "orderNumber", savedOrder.getOrderNumber(),
                "userId", savedOrder.getUserId(),
                "eventId", savedOrder.getEvent().getId(),
                "ticketTierId", savedOrder.getTicketTier().getId(),
                "quantity", savedOrder.getQuantity(),
                "totalAmount", savedOrder.getTotalAmount(),
                "createdAt", LocalDateTime.now().toString()
        ));

        log.info("Order [{}] successfully created from reservation [{}] for user [{}]",
                orderNumber, reservationCode, userId);

        idempotencyService.complete("order:" + reservationCode, java.time.Duration.ofMinutes(30));

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + orderNumber));
    }

    @Transactional(readOnly = true)
    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    private void saveOutboxEvent(String aggregateType, String aggregateId, String eventType, Object payloadObj) {
        OutboxStatus pendingStatus = outboxStatusRepository.findByName("PENDING")
                .orElseThrow(() -> new ResourceNotFoundException("Status PENDING not found for outbox"));

        try {
            String payloadJson = objectMapper.writeValueAsString(payloadObj);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(pendingStatus)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload: {}", e.getMessage(), e);
        }
    }
}
