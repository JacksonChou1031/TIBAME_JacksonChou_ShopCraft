package com.jackson.ecommerce.order.api;

import com.jackson.ecommerce.order.repository.OrderRepository;

import java.util.List;

public record OrderListResponse(List<OrderSummaryResponse> items) {
    public static OrderListResponse from(List<OrderRepository.OrderRow> orders) {
        return new OrderListResponse(orders.stream().map(OrderSummaryResponse::from).toList());
    }
}
