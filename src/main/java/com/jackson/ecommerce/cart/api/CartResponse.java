package com.jackson.ecommerce.cart.api;

import com.jackson.ecommerce.cart.domain.CartItem;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        Long sellerId,
        List<CartItemResponse> items,
        BigDecimal currentTotal,
        boolean checkoutReady
) {
    public static CartResponse from(Long id, Long sellerId, List<CartItem> items) {
        List<CartItemResponse> responses = items.stream().map(CartItemResponse::from).toList();
        BigDecimal total = responses.stream()
                .filter(CartItemResponse::purchasable)
                .map(CartItemResponse::currentLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(id, sellerId, responses, total,
                !responses.isEmpty() && responses.stream().allMatch(CartItemResponse::purchasable));
    }
}
