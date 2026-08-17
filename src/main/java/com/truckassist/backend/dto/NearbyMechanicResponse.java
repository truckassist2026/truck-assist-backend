package com.truckassist.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record NearbyMechanicResponse(

        UUID mechanicId,

        UUID userId,

        String name,

        String phone,

        String workshopName,

        BigDecimal rating,

        Integer totalJobs,

        BigDecimal latitude,

        BigDecimal longitude,

        double distanceKm
) {
}