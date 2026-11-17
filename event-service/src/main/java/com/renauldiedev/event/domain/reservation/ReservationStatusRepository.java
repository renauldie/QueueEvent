package com.renauldiedev.event.domain.reservation;

import com.renauldiedev.event.domain.reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationStatusRepository extends JpaRepository<ReservationStatus, Integer> {
    Optional<ReservationStatus> findByName(String name);
}
