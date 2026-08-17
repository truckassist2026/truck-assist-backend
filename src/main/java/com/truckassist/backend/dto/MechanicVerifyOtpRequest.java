package com.truckassist.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record MechanicVerifyOtpRequest(

        @NotBlank(message = "Phone number is required")
        String phone,

        @NotBlank(message = "OTP is required")
        String otp
) {
}