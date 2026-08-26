package com.jackson.ecommerce.product.api;

import com.jackson.ecommerce.product.domain.ProductImage;

public record ProductImageUploadResponse(ProductImageResponse image) {
    public static ProductImageUploadResponse from(ProductImage image) {
        return new ProductImageUploadResponse(ProductImageResponse.from(image));
    }
}
