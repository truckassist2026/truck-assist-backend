package com.truckassist.backend.dto;

import java.util.UUID;

public record MechanicVehicleResponse(

        UUID id,

        String registrationNumber,

        String manufacturer,

        String model,

        String vehicleType,

        Integer manufacturingYear,

        String color

) {
}