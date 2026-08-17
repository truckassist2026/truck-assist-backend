package com.truckassist.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record DriverRequest(

        @NotNull(message = "User ID is required")
        UUID userId,

        String licenseNumber,

        LocalDate licenseExpiryDate,

        String emergencyContactName,

        String emergencyContactPhone
) {
}