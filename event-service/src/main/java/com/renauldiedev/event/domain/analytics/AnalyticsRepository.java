package com.renauldiedev.event.domain.analytics;

import com.renauldiedev.event.domain.order.Order;
import com.renauldiedev.event.api.dto.analytics.EventRevenueProjection;
import com.renauldiedev.event.api.dto.analytics.ReservationTrendProjection;
import com.renauldiedev.event.api.dto.analytics.TierOccupancyProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Order, Long> {

    @Query(value = """
            with event_sales_summary as (
                select
                    o.event_id,
                    count(o.id) as total_orders,
                    coalesce(sum(o.quantity), 0) as total_tickets_sold,
                    coalesce(sum(o.total_amount), 0.00) as total_revenue
                from orders o
                join order_statuses os on o.status_id = os.id
                where os.name != 'CANCELLED' and os.name != 'FAILED'
                group by o.event_id
            )
            select
                e.id as "eventId",
                e.title as "eventTitle",
                e.category as "category",
                es.name as "status",
                coalesce(s.total_orders, 0) as "totalOrders",
                coalesce(s.total_tickets_sold, 0) as "totalTicketsSold",
                coalesce(s.total_revenue, 0.00) as "totalRevenue",
                dense_rank() over (order by coalesce(s.total_revenue, 0.00) desc) as "revenueRank",
                case
                    when sum(coalesce(s.total_revenue, 0.00)) over () > 0
                    then round((coalesce(s.total_revenue, 0.00) * 100.0) / sum(coalesce(s.total_revenue, 0.00)) over (), 2)
                    else 0.00
                end as "revenuePercentage"
            from events e
            join event_statuses es on e.status_id = es.id
            left join event_sales_summary s on e.id = s.event_id
            order by "revenueRank" asc, e.id asc
            """, nativeQuery = true)
    List<EventRevenueProjection> getEventRevenueRankings();

    @Query(value = """
            with tier_inventory_stats as (
                select
                    t.id as tier_id,
                    t.event_id,
                    t.tier_name,
                    t.price,
                    t.total_capacity,
                    t.available_capacity,
                    (t.total_capacity - t.available_capacity) as sold_tickets
                from ticket_tiers t
            )
            select
                e.id as "eventId",
                e.title as "eventTitle",
                ts.tier_id as "tierId",
                ts.tier_name as "tierName",
                ts.price as "price",
                ts.total_capacity as "totalCapacity",
                ts.available_capacity as "availableCapacity",
                ts.sold_tickets as "soldTickets",
                row_number() over (
                    partition by ts.event_id
                    order by ts.sold_tickets desc, ts.price desc
                ) as "tierRankInEvent",
                round((cast(ts.sold_tickets as decimal) * 100.0) / nullif(ts.total_capacity, 0), 2) as "occupancyRate"
            from tier_inventory_stats ts
            join events e on ts.event_id = e.id
            order by e.id asc, "tierRankInEvent" asc
            """, nativeQuery = true)
    List<TierOccupancyProjection> getTierOccupancyRankings();

    @Query(value = """
            with daily_reservation_summary as (
                select
                    to_char(r.created_at, 'YYYY-MM-DD') as res_date,
                    count(r.id) as daily_cnt
                from reservations r
                group by to_char(r.created_at, 'YYYY-MM-DD')
            )
            select
                d.res_date as "reservationDate",
                d.daily_cnt as "dailyCount",
                sum(d.daily_cnt) over (order by d.res_date asc) as "cumulativeTotal",
                lag(d.daily_cnt, 1, 0) over (order by d.res_date asc) as "previousDayCount"
            from daily_reservation_summary d
            order by d.res_date asc
            """, nativeQuery = true)
    List<ReservationTrendProjection> getDailyReservationTrends();
}
