package com.truckassist.backend.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DriverResponse(
        UUID id,
        UUID userId,
        String name,
        String phone,
        String licenseNumber,
        LocalDate licenseExpiryDate,
        String emergencyContactName,
        String emergencyContactPhone,
        boolean available
) {
}