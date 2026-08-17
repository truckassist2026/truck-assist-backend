package com.truckassist.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MechanicRequest(

        @NotNull(message = "User ID is required")
        UUID userId,

        Integer experienceYears,

        String workshopName,

        String workshopAddress
) {
}