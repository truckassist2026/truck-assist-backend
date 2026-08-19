package com.truckassist.backend.dto;

import java.util.UUID;

public record MechanicDriverResponse(

        UUID id,

        String name,

        String phone,

        String email,

        String profileImageUrl

) {
}