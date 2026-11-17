package com.renauldiedev.event.domain.event;

import com.renauldiedev.event.domain.event.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventStatusRepository extends JpaRepository<EventStatus, Integer> {
    Optional<EventStatus> findByName(String name);
}
