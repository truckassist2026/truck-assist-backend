package com.truckassist.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceRequestResponse(

        UUID id,

        UUID driverId,

        UUID vehicleId,

        String category,

        String description,

        BigDecimal latitude,

        BigDecimal longitude,

        String address,

        String status,

        UUID assignedMechanicId,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt,

        OffsetDateTime completedAt,

        OffsetDateTime cancelledAt,

        VehicleSummaryResponse vehicle,

        MechanicSummaryResponse mechanic
) {
}