package com.truckassist.backend.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String phone,
        String name,
        String email,
        String role,
        String status
) {
}