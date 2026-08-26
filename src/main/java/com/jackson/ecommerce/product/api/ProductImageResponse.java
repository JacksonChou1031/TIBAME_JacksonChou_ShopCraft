package com.jackson.ecommerce.product.api;

import com.jackson.ecommerce.product.domain.ProductImage;

public record ProductImageResponse(
        long id,
        String originalFilename,
        String mediaType,
        long fileSize,
        String url
) {
    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.id(), image.originalFilename(), image.mediaType(), image.fileSize(),
                "/api/v1/products/%d/images/%d".formatted(image.productId(), image.id()));
    }
}
