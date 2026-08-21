package com.truckassist.backend.controller;

import com.truckassist.backend.dto.MechanicAvailabilityRequest;
import com.truckassist.backend.dto.MechanicLocationRequest;
import com.truckassist.backend.dto.MechanicProfileRequest;
import com.truckassist.backend.dto.MechanicResponse;
import com.truckassist.backend.dto.MechanicServiceRequestResponse;
import com.truckassist.backend.dto.NearbyMechanicResponse;
import com.truckassist.backend.dto.ServiceRequestResponse;
import com.truckassist.backend.service.MechanicService;
import com.truckassist.backend.service.ServiceRequestService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/mechanics")
public class MechanicController {


    private final MechanicService service;

    private final ServiceRequestService serviceRequestService;


    public MechanicController(
            MechanicService service,
            ServiceRequestService serviceRequestService) {

        this.service =
                service;

        this.serviceRequestService =
                serviceRequestService;
    }


    // =====================================================
    // MY PROFILE
    // =====================================================

    @GetMapping("/me")
    @Operation(
            summary =
                    "Get current mechanic profile"
    )
    public MechanicResponse getMe(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.getMe(
                userId
        );
    }


    // =====================================================
    // CREATE / UPDATE PROFILE
    // =====================================================

    @PutMapping("/me")
    @Operation(
            summary =
                    "Create or update mechanic profile"
    )
    public MechanicResponse updateMe(
            Authentication authentication,
            @Valid
            @RequestBody
            MechanicProfileRequest request) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.updateMe(
                userId,
                request
        );
    }


    // =====================================================
    // AVAILABILITY
    // =====================================================

    @PatchMapping("/me/availability")
    @Operation(
            summary =
                    "Update mechanic availability"
    )
    public MechanicResponse updateAvailability(
            Authentication authentication,
            @Valid
            @RequestBody
            MechanicAvailabilityRequest request) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.updateAvailability(
                userId,
                request
        );
    }


    // =====================================================
    // CURRENT LOCATION
    // =====================================================

    @PatchMapping("/me/location")
    @Operation(
            summary =
                    "Update mechanic current location"
    )
    public MechanicResponse updateLocation(
            Authentication authentication,
            @Valid
            @RequestBody
            MechanicLocationRequest request) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.updateLocation(
                userId,
                request
        );
    }


    // =====================================================
    // NEARBY MECHANICS
    // =====================================================

    @GetMapping("/nearby")
    @Operation(
            summary =
                    "Find nearby available mechanics"
    )
    public List<NearbyMechanicResponse> getNearby(
            @RequestParam
            BigDecimal latitude,

            @RequestParam
            BigDecimal longitude,

            @RequestParam(
                    defaultValue = "10"
            )
            double radiusKm) {

        return service.getNearbyMechanics(
                latitude,
                longitude,
                radiusKm
        );
    }


    // =====================================================
    // AVAILABLE SERVICE REQUESTS
    // =====================================================

    @GetMapping("/requests")
    @Operation(
            summary =
                    "Get service requests available for current mechanic"
    )
    public List<MechanicServiceRequestResponse> getRequests(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return serviceRequestService
                .getAvailableRequests(
                        userId
                );
    }


    // =====================================================
    // GET SINGLE SERVICE REQUEST
    // =====================================================

    @GetMapping(
            "/requests/{requestId}"
    )
    @Operation(
            summary =
                    "Get service request details"
    )
    public MechanicServiceRequestResponse getRequestById(
            Authentication authentication,

            @PathVariable
            UUID requestId) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return serviceRequestService
                .getRequestByIdForMechanic(
                        userId,
                        requestId
                );
    }


        // =====================================================
    // ACCEPT SERVICE REQUEST
    // =====================================================

    @PatchMapping(
            "/requests/{requestId}/accept"
    )
    @Operation(
            summary =
                    "Accept a service request"
    )
    public ServiceRequestResponse acceptRequest(
            Authentication authentication,

            @PathVariable
            UUID requestId) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return serviceRequestService
                .acceptRequest(
                        userId,
                        requestId
                );
    }


    // =====================================================
    // UPDATE SERVICE REQUEST STATUS
    // =====================================================

    @PatchMapping(
            "/requests/{requestId}/status"
    )
    @Operation(
            summary =
                    "Update service request status"
    )
    public ServiceRequestResponse updateRequestStatus(
            Authentication authentication,

            @PathVariable
            UUID requestId,

            @RequestParam
            String status) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return serviceRequestService
                .updateMechanicStatus(
                        userId,
                        requestId,
                        status
                );
    }

}