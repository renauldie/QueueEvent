package com.renauldiedev.event.domain.outbox;

import com.renauldiedev.event.domain.outbox.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status.name = :statusName ORDER BY o.createdAt ASC")
    List<OutboxEvent> findByStatusNameOrderByCreatedAtAsc(@Param("statusName") String statusName, Pageable pageable);
}
