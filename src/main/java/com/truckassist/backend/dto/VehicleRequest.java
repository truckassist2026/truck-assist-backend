package com.truckassist.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleRequest(

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