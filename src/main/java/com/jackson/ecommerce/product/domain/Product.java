package com.jackson.ecommerce.product.domain;

import java.math.BigDecimal;
import java.util.List;

public record Product(
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
        List<ProductImage> images
) {
    public boolean isPubliclyVisible() {
        return status == ProductStatus.PUBLISHED && !deleted;
    }
}
