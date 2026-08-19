package com.truckassist.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MechanicServiceRequestResponse(

        UUID id,

        UUID driverId,

        UUID vehicleId,

        String category,

        String description,

        BigDecimal latitude,

        BigDecimal longitude,

        String address,

        String status,

        OffsetDateTime createdAt,

        double distanceKm,

        MechanicDriverResponse driver,

        MechanicVehicleResponse vehicle

) {
}