package com.jackson.ecommerce.member.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(max = 30) String phone
) {
}
