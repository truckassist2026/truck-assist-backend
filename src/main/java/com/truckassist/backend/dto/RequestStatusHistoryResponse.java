package com.truckassist.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestStatusHistoryResponse(

        UUID id,

        UUID requestId,

        String status,

        UUID changedByUserId,

        String notes,

        OffsetDateTime createdAt
) {
}