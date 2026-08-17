package com.truckassist.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[0-9+ ]{8,20}$",
                message = "Invalid phone number"
        )
        String phone,

        String name,

        String email,

        String role
) {
}