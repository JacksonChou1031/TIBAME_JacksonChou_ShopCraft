package com.jackson.ecommerce.order.api;

import java.math.BigDecimal;

public record CheckoutResponse(
        Long orderId,
        String orderStatus,
        String paymentStatus,
        String shippingMethod,
        BigDecimal subtotalAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        String idempotencyKey,
        boolean replayed,
        String message
) {
}
