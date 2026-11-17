package com.renauldiedev.event.api.controller;

import com.renauldiedev.event.domain.reservation.Reservation;
import com.renauldiedev.event.api.dto.ApiResponse;
import com.renauldiedev.event.api.dto.CreateReservationRequest;
import com.renauldiedev.event.api.dto.ReservationResponse;
import com.renauldiedev.event.domain.reservation.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservation Management", description = "Temporary seat reservation and hold endpoints")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create a temporary seat reservation with 10-minute hold TTL")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody CreateReservationRequest request) {

        Reservation reservation = reservationService.createReservation(
                userId,
                request.getEventId(),
                request.getTicketTierId(),
                request.getQuantity()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Temporary reservation hold created. Valid for 10 minutes.", mapToReservationResponse(reservation)));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get reservation details and hold status by reservation code")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(@PathVariable("code") String code) {
        Reservation reservation = reservationService.getReservation(code);
        return ResponseEntity.ok(ApiResponse.success(mapToReservationResponse(reservation)));
    }

    private ReservationResponse mapToReservationResponse(Reservation reservation) {
        BigDecimal unitPrice = reservation.getTicketTier().getPrice();
        BigDecimal estimatedTotal = unitPrice.multiply(BigDecimal.valueOf(reservation.getQuantity()));

        return ReservationResponse.builder()
                .id(reservation.getId())
                .reservationCode(reservation.getReservationCode())
                .userId(reservation.getUserId())
                .eventId(reservation.getEvent().getId())
                .eventTitle(reservation.getEvent().getTitle())
                .ticketTierId(reservation.getTicketTier().getId())
                .tierName(reservation.getTicketTier().getTierName())
                .unitPrice(unitPrice)
                .quantity(reservation.getQuantity())
                .estimatedTotal(estimatedTotal)
                .expiresAt(reservation.getExpiresAt())
                .status(reservation.getStatus().getName())
                .build();
    }
}
