package com.renauldiedev.event.domain.event;

import com.renauldiedev.event.domain.event.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.ticketTiers WHERE e.id = :id")
    Optional<Event> findByIdWithTiers(@Param("id") Long id);

    Page<Event> findByStatusName(String statusName, Pageable pageable);

    Page<Event> findByCategoryIgnoreCase(String category, Pageable pageable);
}
