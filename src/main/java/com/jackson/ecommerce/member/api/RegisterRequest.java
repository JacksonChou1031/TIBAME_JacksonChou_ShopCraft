package com.jackson.ecommerce.member.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{3,50}$", message = "username must contain 3-50 letters, digits, '.', '_' or '-'") String username,
        @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$", message = "password must be 8-72 characters and contain letters and digits") String password,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(max = 30) String phone
) {
}
