package com.truckassist.backend.dto;

import java.time.LocalDate;

public record DriverUpdateRequest(

        String name,

        String email,

        String profileImageUrl,

        String licenseNumber,

        LocalDate licenseExpiryDate,

        String emergencyContactName,

        String emergencyContactPhone

) {
}