package com.truckassist.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentMethodRequest(

        @NotBlank(
                message = "Payment method is required"
        )
        String paymentMethod
) {
}