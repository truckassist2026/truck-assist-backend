package com.truckassist.backend.dto.auth;

public record SendOtpResponse(
        boolean success,
        String message,
        Integer expiresInSeconds,
        String developmentOtp
) {
}