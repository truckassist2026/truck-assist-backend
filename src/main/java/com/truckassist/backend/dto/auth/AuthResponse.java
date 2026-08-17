package com.truckassist.backend.dto.auth;

import java.util.UUID;

public record AuthResponse(

        String accessToken,

        String tokenType,

        long expiresInSeconds,

        UUID userId,

        String role,

        boolean newUser

) {
}