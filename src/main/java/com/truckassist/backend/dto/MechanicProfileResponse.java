package com.truckassist.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MechanicProfileResponse(

        UUID userId,

        UUID mechanicId,

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

        boolean profileCompleted,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt
) {
}