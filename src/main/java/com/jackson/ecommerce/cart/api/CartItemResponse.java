package com.jackson.ecommerce.cart.api;

import com.jackson.ecommerce.cart.domain.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        long id,
        long productId,
        long sellerId,
        String productName,
        BigDecimal currentUnitPrice,
        String currency,
        int quantity,
        int currentStock,
        String productStatus,
        boolean deleted,
        boolean purchasable,
        String unavailableReason,
        BigDecimal currentLineTotal
) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.id(), item.productId(), item.sellerId(), item.productName(), item.currentUnitPrice(),
                item.currency(), item.quantity(), item.currentStock(), item.productStatus().name(), item.deleted(),
                item.purchasable(), item.unavailableReason(),
                item.currentUnitPrice().multiply(BigDecimal.valueOf(item.quantity())));
    }
}
