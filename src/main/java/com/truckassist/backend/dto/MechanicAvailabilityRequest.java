package com.truckassist.backend.dto;

import jakarta.validation.constraints.NotNull;

public record MechanicAvailabilityRequest(

        @NotNull
        Boolean available
) {
}