package com.jackson.ecommerce.order.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipmentRequest(
        @NotBlank @Size(max = 100) String trackingNumber
) {
}
