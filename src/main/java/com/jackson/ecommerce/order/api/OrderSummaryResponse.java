package com.jackson.ecommerce.order.api;

import com.jackson.ecommerce.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(long orderId, long buyerId, long sellerId, String status,
                                   BigDecimal subtotalAmount, BigDecimal shippingFee, BigDecimal totalAmount,
                                   String shippingMethod, String trackingNumber,
                                   Instant createdAt, Instant updatedAt) {
    public static OrderSummaryResponse from(OrderRepository.OrderRow order) {
        return new OrderSummaryResponse(order.orderId(), order.buyerId(), order.sellerId(), order.status().name(),
                order.subtotal(), order.shippingFee(), order.total(), order.shippingMethod().name(),
                order.trackingNumber(), order.createdAt(), order.updatedAt());
    }
}
