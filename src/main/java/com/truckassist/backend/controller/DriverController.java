package com.truckassist.backend.controller;

import com.truckassist.backend.dto.DriverRequest;
import com.truckassist.backend.dto.DriverResponse;
import com.truckassist.backend.dto.DriverUpdateRequest;
import com.truckassist.backend.service.DriverService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService service;

    public DriverController(
            DriverService service) {

        this.service = service;
    }

    // =====================================================
    // GET CURRENT DRIVER
    // =====================================================

    @GetMapping("/me")
    @Operation(
            summary = "Get current driver's profile"
    )
    public DriverResponse getMe(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.getByUserId(
                userId
        );
    }

    // =====================================================
    // UPDATE CURRENT DRIVER
    // =====================================================

    @PutMapping("/me")
    @Operation(
            summary = "Update current driver's profile"
    )
    public DriverResponse updateMe(
            Authentication authentication,
            @RequestBody DriverUpdateRequest request) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.updateByUserId(
                userId,
                request
        );
    }

    // =====================================================
    // UPDATE AVAILABILITY
    // =====================================================

    @PatchMapping("/me/availability")
    @Operation(
            summary = "Update driver availability"
    )
    public DriverResponse updateAvailability(
            Authentication authentication,
            @RequestParam boolean available) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.updateAvailability(
                userId,
                available
        );
    }

    // =====================================================
    // CREATE DRIVER PROFILE
    // =====================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create driver profile"
    )
    public DriverResponse create(
            @Valid @RequestBody DriverRequest request) {

        return service.create(
                request
        );
    }

    // =====================================================
    // GET DRIVER BY ID
    // =====================================================

    @GetMapping("/{id}")
    @Operation(
            summary = "Get driver by ID"
    )
    public DriverResponse get(
            @PathVariable UUID id) {

        return service.getById(id);
    }
}