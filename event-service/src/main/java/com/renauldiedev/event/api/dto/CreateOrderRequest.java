package com.renauldiedev.event.api.dto;

import com.renauldiedev.event.domain.reservation.Reservation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Reservation code is required")
    private String reservationCode;
}
