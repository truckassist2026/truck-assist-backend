package com.truckassist.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(

        UUID id,

        UUID serviceRequestId,

        BigDecimal amount,

        String status,

        String paymentMethod,

        String notes,

        OffsetDateTime createdAt,

        OffsetDateTime paidAt
) {
}