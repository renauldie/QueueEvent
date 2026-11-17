package com.renauldiedev.event.infrastructure.config;

import com.renauldiedev.event.domain.event.Event;
import com.renauldiedev.event.domain.event.EventStatus;
import com.renauldiedev.event.domain.inventory.SeatInventory;
import com.renauldiedev.event.domain.inventory.SeatStatus;
import com.renauldiedev.event.domain.inventory.TicketTier;
import com.renauldiedev.event.domain.order.OrderStatus;
import com.renauldiedev.event.domain.outbox.OutboxStatus;
import com.renauldiedev.event.domain.reservation.ReservationStatus;
import com.renauldiedev.event.domain.event.EventRepository;
import com.renauldiedev.event.domain.event.EventStatusRepository;
import com.renauldiedev.event.domain.inventory.SeatInventoryRepository;
import com.renauldiedev.event.domain.inventory.SeatStatusRepository;
import com.renauldiedev.event.domain.inventory.TicketTierRepository;
import com.renauldiedev.event.domain.reservation.ReservationStatusRepository;
import com.renauldiedev.event.domain.order.OrderStatusRepository;
import com.renauldiedev.event.domain.outbox.OutboxStatusRepository;
import com.renauldiedev.event.infrastructure.elasticsearch.service.EventSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final EventStatusRepository eventStatusRepository;
    private final SeatStatusRepository seatStatusRepository;
    private final ReservationStatusRepository reservationStatusRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final OutboxStatusRepository outboxStatusRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final EventSearchService eventSearchService;

    public DatabaseSeeder(
            EventStatusRepository eventStatusRepository,
            SeatStatusRepository seatStatusRepository,
            ReservationStatusRepository reservationStatusRepository,
            OrderStatusRepository orderStatusRepository,
            OutboxStatusRepository outboxStatusRepository,
            EventRepository eventRepository,
            TicketTierRepository ticketTierRepository,
            SeatInventoryRepository seatInventoryRepository,
            EventSearchService eventSearchService) {
        this.eventStatusRepository = eventStatusRepository;
        this.seatStatusRepository = seatStatusRepository;
        this.reservationStatusRepository = reservationStatusRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.outboxStatusRepository = outboxStatusRepository;
        this.eventRepository = eventRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.eventSearchService = eventSearchService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting database initialization and seeding...");

        seedEventStatuses();
        seedSeatStatuses();
        seedReservationStatuses();
        seedOrderStatuses();
        seedOutboxStatuses();
        seedSampleEvent();

        log.info("Database initialization and seeding completed successfully.");
    }

    private void seedEventStatuses() {
        if (eventStatusRepository.count() == 0) {
            eventStatusRepository.saveAll(List.of(
                    EventStatus.builder().name("DRAFT").description("Event in creation").build(),
                    EventStatus.builder().name("PUBLISHED").description("Event open for reservation and ticket booking").build(),
                    EventStatus.builder().name("COMPLETED").description("Event concluded").build(),
                    EventStatus.builder().name("CANCELLED").description("Event cancelled by organizer").build()
            ));
            log.info("Seeded default event statuses into 'event_statuses' table.");
        }
    }

    private void seedSeatStatuses() {
        if (seatStatusRepository.count() == 0) {
            seatStatusRepository.saveAll(List.of(
                    SeatStatus.builder().name("AVAILABLE").description("Seat is available for reservation").build(),
                    SeatStatus.builder().name("HELD").description("Seat temporarily held in active reservation TTL").build(),
                    SeatStatus.builder().name("RESERVED").description("Seat reserved awaiting payment completion").build(),
                    SeatStatus.builder().name("BOOKED").description("Seat fully paid and confirmed").build()
            ));
            log.info("Seeded default seat statuses into 'seat_statuses' table.");
        }
    }

    private void seedReservationStatuses() {
        if (reservationStatusRepository.count() == 0) {
            reservationStatusRepository.saveAll(List.of(
                    ReservationStatus.builder().name("PENDING").description("Temporary hold active").build(),
                    ReservationStatus.builder().name("CONFIRMED").description("Reservation converted to order").build(),
                    ReservationStatus.builder().name("EXPIRED").description("Reservation hold timed out").build(),
                    ReservationStatus.builder().name("CANCELLED").description("Reservation cancelled by user").build()
            ));
            log.info("Seeded default reservation statuses into 'reservation_statuses' table.");
        }
    }

    private void seedOrderStatuses() {
        if (orderStatusRepository.count() == 0) {
            orderStatusRepository.saveAll(List.of(
                    OrderStatus.builder().name("PENDING_PAYMENT").description("Order placed awaiting payment").build(),
                    OrderStatus.builder().name("PAID").description("Payment verified successfully").build(),
                    OrderStatus.builder().name("CANCELLED").description("Order cancelled").build(),
                    OrderStatus.builder().name("FAILED").description("Order payment failed").build()
            ));
            log.info("Seeded default order statuses into 'order_statuses' table.");
        }
    }

    private void seedOutboxStatuses() {
        if (outboxStatusRepository.count() == 0) {
            outboxStatusRepository.saveAll(List.of(
                    OutboxStatus.builder().name("PENDING").description("Outbox message awaiting Kafka publication").build(),
                    OutboxStatus.builder().name("PROCESSED").description("Message successfully published to Kafka").build(),
                    OutboxStatus.builder().name("FAILED").description("Message delivery to Kafka failed after retries").build()
            ));
            log.info("Seeded default outbox statuses into 'outbox_statuses' table.");
        }
    }

    private void seedSampleEvent() {
        if (eventRepository.count() == 0) {
            EventStatus publishedStatus = eventStatusRepository.findByName("PUBLISHED").orElseThrow();
            SeatStatus availableStatus = seatStatusRepository.findByName("AVAILABLE").orElseThrow();

            Event sampleEvent = Event.builder()
                    .title("Coldplay Music of the Spheres World Tour")
                    .description("Live in Gelora Bung Karno Stadium Jakarta. Experience the spectacular world tour concert.")
                    .category("CONCERT")
                    .location("Gelora Bung Karno Stadium, Jakarta")
                    .eventDate(LocalDateTime.now().plusMonths(2))
                    .status(publishedStatus)
                    .build();

            TicketTier vipTier = TicketTier.builder()
                    .tierName("VIP Experience")
                    .price(new BigDecimal("3500000.00"))
                    .totalCapacity(50)
                    .availableCapacity(50)
                    .build();

            TicketTier cat1Tier = TicketTier.builder()
                    .tierName("CAT 1 Numbered Seating")
                    .price(new BigDecimal("1850000.00"))
                    .totalCapacity(100)
                    .availableCapacity(100)
                    .build();

            sampleEvent.addTicketTier(vipTier);
            sampleEvent.addTicketTier(cat1Tier);

            Event savedEvent = eventRepository.save(sampleEvent);

            for (int i = 1; i <= 50; i++) {
                seatInventoryRepository.save(SeatInventory.builder()
                        .ticketTier(vipTier)
                        .seatNumber(String.format("VIP-%02d", i))
                        .status(availableStatus)
                        .build());
            }

            log.info("Seeded sample event: '{}' with 2 ticket tiers and 50 VIP seats.", savedEvent.getTitle());
            eventSearchService.indexEvent(savedEvent);
        }
    }
}
