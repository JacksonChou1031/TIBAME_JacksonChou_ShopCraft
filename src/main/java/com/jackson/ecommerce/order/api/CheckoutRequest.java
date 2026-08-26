package com.jackson.ecommerce.order.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotBlank String shippingMethod,
        @NotBlank @Size(max = 100) String recipientName,
        @NotBlank @Size(max = 30) String recipientPhone,
        @Size(max = 100) String storeName,
        @Size(max = 30) String storeCode,
        @Size(max = 300) String deliveryAddress,
        @NotBlank @Size(max = 100) String mockAccountName,
        @NotBlank @Size(max = 100) String mockAccountNumber
) {
}
