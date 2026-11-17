package com.renauldiedev.event.domain.inventory;

import com.renauldiedev.event.domain.inventory.SeatInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    List<SeatInventory> findByTicketTierIdAndStatusName(Long ticketTierId, String statusName);

    Optional<SeatInventory> findByTicketTierIdAndSeatNumber(Long ticketTierId, String seatNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatInventory s WHERE s.ticketTier.id = :ticketTierId AND s.status.name = 'AVAILABLE' ORDER BY s.id ASC")
    List<SeatInventory> findAvailableSeatsForReservation(@Param("ticketTierId") Long ticketTierId, Pageable pageable);
}
