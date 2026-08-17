package com.truckassist.backend.controller;

import com.truckassist.backend.dto.CreateServiceRequestRequest;
import com.truckassist.backend.dto.RequestStatusHistoryResponse;
import com.truckassist.backend.dto.ServiceRequestResponse;
import com.truckassist.backend.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(
            ServiceRequestService service) {

        this.service = service;
    }

    // =====================================================
    // CREATE
    // =====================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new breakdown service request"
    )
    public ServiceRequestResponse create(
            Authentication authentication,
            @Valid @RequestBody
            CreateServiceRequestRequest request) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.create(
                userId,
                request
        );
    }

    // =====================================================
    // MY REQUESTS
    // =====================================================

    @GetMapping("/my")
    @Operation(
            summary = "Get current driver's service requests"
    )
    public List<ServiceRequestResponse> getMyRequests(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.getMyRequests(userId);
    }

    // =====================================================
    // ACTIVE REQUEST
    // =====================================================

    @GetMapping("/active")
    @Operation(
            summary = "Get current driver's active request"
    )
    public ServiceRequestResponse getActive(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.getActiveRequest(userId);
    }

    // =====================================================
    // REQUEST DETAILS
    // =====================================================

    @GetMapping("/{id}")
    @Operation(
            summary = "Get service request details"
    )
    public ServiceRequestResponse getById(
            Authentication authentication,
            @PathVariable UUID id) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.getById(
                userId,
                id
        );
    }

    // =====================================================
    // REQUEST HISTORY
    // =====================================================

    @GetMapping("/{id}/history")
    @Operation(
            summary = "Get service request status history"
    )
    public List<RequestStatusHistoryResponse> getHistory(
            Authentication authentication,
            @PathVariable UUID id) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.getHistory(
                userId,
                id
        );
    }

    // =====================================================
    // CANCEL
    // =====================================================

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel service request"
    )
    public ServiceRequestResponse cancel(
            Authentication authentication,
            @PathVariable UUID id) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.cancel(
                userId,
                id
        );
    }
}