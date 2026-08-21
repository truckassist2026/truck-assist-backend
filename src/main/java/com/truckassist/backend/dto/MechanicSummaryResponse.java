package com.truckassist.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MechanicSummaryResponse(

        UUID id,

        String name,

        String phone,

        String profileImageUrl,

        Integer experienceYears,

        String workshopName,

        String workshopAddress,

        BigDecimal rating,

        Integer totalJobs,

        BigDecimal latitude,

        BigDecimal longitude,

        OffsetDateTime lastLocationAt
) {
}