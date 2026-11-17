package com.renauldiedev.event.domain.inventory;

import com.renauldiedev.event.domain.inventory.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeatStatusRepository extends JpaRepository<SeatStatus, Integer> {
    Optional<SeatStatus> findByName(String name);
}
