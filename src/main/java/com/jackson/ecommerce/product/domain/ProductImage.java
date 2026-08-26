package com.jackson.ecommerce.product.domain;

public record ProductImage(
        long id,
        long productId,
        String storageKey,
        String originalFilename,
        String mediaType,
        long fileSize,
        int sortOrder
) {
}
