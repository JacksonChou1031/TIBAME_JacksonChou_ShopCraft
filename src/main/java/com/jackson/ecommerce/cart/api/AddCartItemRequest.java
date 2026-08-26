package com.jackson.ecommerce.cart.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull @Positive Long productId,
        @Positive int quantity
) {
}
