package com.truckassist.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VehicleRequest(

        @NotNull(message = "Driver ID is required")
        UUID driverId,

        @NotBlank(message = "Registration number is required")
        String registrationNumber,

        String manufacturer,

        String model,

        String vehicleType,

        Integer manufacturingYear,

        String color,

        Boolean primary
) {
}