package com.renauldiedev.event.domain.outbox;

import com.renauldiedev.event.domain.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OutboxStatusRepository extends JpaRepository<OutboxStatus, Integer> {
    Optional<OutboxStatus> findByName(String name);
}
