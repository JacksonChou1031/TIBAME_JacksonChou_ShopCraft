package com.jackson.ecommerce.cart.api;

import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(@Positive int quantity) {
}
