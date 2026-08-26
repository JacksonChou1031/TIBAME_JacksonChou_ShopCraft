package com.jackson.ecommerce.order.api;

import com.jackson.ecommerce.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(long orderId, long buyerId, long sellerId, String status,
                            BigDecimal subtotalAmount, BigDecimal shippingFee, BigDecimal totalAmount,
                            String shippingMethod, String recipientName, String recipientPhone,
                            String storeName, String storeCode, String deliveryAddress,
                            String trackingNumber, Instant createdAt, Instant updatedAt,
                            List<OrderItemResponse> items,
                            List<OrderStatusHistoryResponse> statusHistory) {
    public static OrderResponse from(OrderRepository.OrderRow order,
                                     List<OrderRepository.OrderItemRow> items,
                                     List<OrderRepository.StatusHistoryRow> history) {
        return new OrderResponse(order.orderId(), order.buyerId(), order.sellerId(), order.status().name(),
                order.subtotal(), order.shippingFee(), order.total(), order.shippingMethod().name(),
                order.recipientName(), order.recipientPhone(), order.storeName(), order.storeCode(),
                order.deliveryAddress(), order.trackingNumber(), order.createdAt(), order.updatedAt(),
                items.stream().map(OrderItemResponse::from).toList(),
                history.stream().map(OrderStatusHistoryResponse::from).toList());
    }
}
