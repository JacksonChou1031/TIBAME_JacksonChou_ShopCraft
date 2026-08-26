package com.jackson.ecommerce.product.api;

import com.jackson.ecommerce.product.domain.Product;
import com.jackson.ecommerce.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        long id,
        long sellerId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int stock,
        String category,
        ProductStatus status,
        boolean deleted,
        List<ProductImageResponse> images
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.id(), product.sellerId(), product.name(), product.description(), product.price(),
                product.currency(), product.stock(), product.category(), product.status(), product.deleted(),
                product.images().stream().map(ProductImageResponse::from).toList());
    }
}
