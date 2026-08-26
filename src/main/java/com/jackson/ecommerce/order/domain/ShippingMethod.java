package com.jackson.ecommerce.order.domain;

import com.jackson.ecommerce.common.web.BadRequestException;

import java.math.BigDecimal;

public enum ShippingMethod {
    CONVENIENCE_STORE("超商取貨", new BigDecimal("60.00")),
    HOME_DELIVERY("宅配到府", new BigDecimal("100.00"));

    private final String displayName;
    private final BigDecimal fee;

    ShippingMethod(String displayName, BigDecimal fee) {
        this.displayName = displayName;
        this.fee = fee;
    }

    public BigDecimal fee() {
        return fee;
    }

    public static ShippingMethod from(String value) {
        try {
            return value == null ? null : valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("shippingMethod must be CONVENIENCE_STORE or HOME_DELIVERY");
        }
    }
}
