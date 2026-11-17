package com.renauldiedev.event.api.controller;

import com.renauldiedev.event.domain.order.Order;
import com.renauldiedev.event.api.dto.ApiResponse;
import com.renauldiedev.event.api.dto.CreateOrderRequest;
import com.renauldiedev.event.api.dto.OrderResponse;
import com.renauldiedev.event.domain.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management", description = "Order placement and customer order history")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Convert an active temporary reservation into a confirmed order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody CreateOrderRequest request) {

        Order order = orderService.createOrderFromReservation(request.getReservationCode(), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully awaiting payment", mapToOrderResponse(order)));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order details by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(@PathVariable("orderNumber") String orderNumber) {
        Order order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(mapToOrderResponse(order)));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "List orders belonging to the authenticated user")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderService.getUserOrders(userId, pageRequest);
        Page<OrderResponse> responsePage = orders.map(this::mapToOrderResponse);

        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .eventId(order.getEvent().getId())
                .eventTitle(order.getEvent().getTitle())
                .ticketTierId(order.getTicketTier().getId())
                .tierName(order.getTicketTier().getTierName())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().getName())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
