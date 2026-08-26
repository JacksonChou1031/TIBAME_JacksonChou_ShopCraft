package com.jackson.ecommerce.cart.domain;

import com.jackson.ecommerce.product.domain.ProductStatus;

import java.math.BigDecimal;

public record CartItem(
        long id,
        long productId,
        long sellerId,
        String productName,
        BigDecimal currentUnitPrice,
        String currency,
        int quantity,
        int currentStock,
        ProductStatus productStatus,
        boolean deleted
) {
    public boolean purchasable() {
        return productStatus == ProductStatus.PUBLISHED && !deleted && currentStock >= quantity;
    }

    public String unavailableReason() {
        if (deleted) {
            return "DELETED";
        }
        if (productStatus != ProductStatus.PUBLISHED) {
            return "UNPUBLISHED";
        }
        if (currentStock == 0) {
            return "SOLD_OUT";
        }
        if (currentStock < quantity) {
            return "INSUFFICIENT_STOCK";
        }
        return null;
    }
}
