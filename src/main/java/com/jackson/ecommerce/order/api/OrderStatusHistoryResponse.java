package com.jackson.ecommerce.order.api;

import com.jackson.ecommerce.order.repository.OrderRepository;

import java.time.Instant;

public record OrderStatusHistoryResponse(String status, long changedBy, Instant changedAt) {
    public static OrderStatusHistoryResponse from(OrderRepository.StatusHistoryRow history) {
        return new OrderStatusHistoryResponse(history.status().name(), history.changedBy(), history.changedAt());
    }
}
