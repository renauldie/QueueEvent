package com.renauldiedev.event.domain.event;

import com.renauldiedev.event.domain.common.BaseEntity;
import com.renauldiedev.event.domain.inventory.TicketTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_category", columnList = "category"),
        @Index(name = "idx_event_date", columnList = "event_date")
})
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private EventStatus status;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketTier> ticketTiers = new ArrayList<>();

    public void addTicketTier(TicketTier tier) {
        ticketTiers.add(tier);
        tier.setEvent(this);
    }

    public void removeTicketTier(TicketTier tier) {
        ticketTiers.remove(tier);
        tier.setEvent(null);
    }
}
