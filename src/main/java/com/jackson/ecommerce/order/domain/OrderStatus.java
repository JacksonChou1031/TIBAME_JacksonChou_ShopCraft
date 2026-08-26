package com.jackson.ecommerce.order.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PENDING_SHIPMENT,
    SHIPPED,
    COMPLETED,
    CANCELLED
}
