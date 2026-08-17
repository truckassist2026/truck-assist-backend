package com.truckassist.backend.dto;

import java.util.UUID;

public record VehicleResponse(
        UUID id,
        UUID driverId,
        String registrationNumber,
        String manufacturer,
        String model,
        String vehicleType,
        Integer manufacturingYear,
        String color,
        boolean primary,
        String status
) {
}