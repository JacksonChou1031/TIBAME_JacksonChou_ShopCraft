package com.jackson.ecommerce.order.api;

import com.jackson.ecommerce.order.repository.OrderRepository;

import java.math.BigDecimal;

public record OrderItemResponse(long orderItemId, long productId, long sellerId, String productName,
                                BigDecimal unitPrice, int quantity, BigDecimal lineAmount) {
    public static OrderItemResponse from(OrderRepository.OrderItemRow item) {
        return new OrderItemResponse(item.orderItemId(), item.productId(), item.sellerId(), item.productName(),
                item.unitPrice(), item.quantity(), item.lineAmount());
    }
}
