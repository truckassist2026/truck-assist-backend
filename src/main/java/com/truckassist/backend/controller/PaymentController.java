package com.truckassist.backend.controller;

import com.truckassist.backend.dto.PaymentInitiateRequest;
import com.truckassist.backend.dto.PaymentMethodRequest;
import com.truckassist.backend.dto.PaymentResponse;
import com.truckassist.backend.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;


    public PaymentController(
            PaymentService service) {

        this.service = service;
    }


    // =====================================================
    // MECHANIC - CREATE PAYMENT
    // =====================================================

    @PostMapping(
            "/requests/{requestId}/initiate"
    )
    @Operation(
            summary = "Create service payment"
    )
    public PaymentResponse initiatePayment(
            Authentication authentication,
            @PathVariable UUID requestId,
            @Valid @RequestBody
            PaymentInitiateRequest request) {
                 System.out.println(
            "================================================="
    );

    System.out.println(
            "[PAYMENT CONTROLLER] initiatePayment() HIT"
    );

    System.out.println(
            "[PAYMENT CONTROLLER] Request ID: " +
            requestId
    );

    System.out.println(
            "[PAYMENT CONTROLLER] Authentication: " +
            authentication
    );

    System.out.println(
            "[PAYMENT CONTROLLER] User ID: " +
            authentication.getName()
    );

    System.out.println(
            "[PAYMENT CONTROLLER] Amount: " +
            request.getAmount()
    );

    System.out.println(
            "[PAYMENT CONTROLLER] Notes: " +
            request.getNotes()
    );

    System.out.println(
            "================================================="
    );

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );


        return service.initiatePayment(
                userId,
                requestId,
                request
        );
    }


    // =====================================================
    // GET PAYMENT
    // =====================================================

    @GetMapping(
            "/requests/{requestId}"
    )
    @Operation(
            summary = "Get payment for service request"
    )
    public PaymentResponse getPayment(
            Authentication authentication,
            @PathVariable UUID requestId) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );


        return service.getPayment(
                userId,
                requestId
        );
    }


    // =====================================================
    // DRIVER - PAY
    // =====================================================

    @PostMapping(
            "/requests/{requestId}/pay"
    )
    @Operation(
            summary = "Pay service request"
    )
    public PaymentResponse pay(
            Authentication authentication,
            @PathVariable UUID requestId,
            @Valid @RequestBody
            PaymentMethodRequest request) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );


        return service.pay(
                userId,
                requestId,
                request
        );
    }
}