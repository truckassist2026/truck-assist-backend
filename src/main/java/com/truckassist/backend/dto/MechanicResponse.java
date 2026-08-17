package com.truckassist.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MechanicResponse(

        UUID id,

        UUID userId,

        String name,

        String phone,

        String email,

        String profileImageUrl,

        Integer experienceYears,

        String workshopName,

        String workshopAddress,

        boolean available,

        BigDecimal rating,

        Integer totalJobs,

        BigDecimal latitude,

        BigDecimal longitude,

        OffsetDateTime lastLocationAt
) {
}