package com.truckassist.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;

public record MechanicProfileRequest(

        String name,

        @Email(message = "Invalid email address")
        String email,

        @Min(value = 0, message = "Experience cannot be negative")
        Integer experienceYears,

        String workshopName,

        String workshopAddress
) {
}