package com.truckassist.backend.controller;

import com.truckassist.backend.dto.VehicleRequest;
import com.truckassist.backend.dto.VehicleResponse;
import com.truckassist.backend.service.VehicleService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(
            VehicleService service) {

        this.service = service;
    }

    // =====================================================
    // MY VEHICLES
    // =====================================================

    @GetMapping("/my")
    @Operation(
            summary = "Get current driver's vehicles"
    )
    public List<VehicleResponse> getMyVehicles(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.getMyVehicles(
                userId
        );
    }

    // =====================================================
    // CREATE VEHICLE
    // =====================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create vehicle"
    )
    public VehicleResponse create(
            Authentication authentication,
            @Valid @RequestBody
            VehicleRequest request) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.createForDriver(
                userId,
                request
        );
    }

    // =====================================================
    // GET VEHICLE BY ID
    // =====================================================

    @GetMapping("/{id}")
    @Operation(
            summary = "Get vehicle by ID"
    )
    public VehicleResponse get(
            @PathVariable UUID id) {

        return service.getById(id);
    }

    // =====================================================
    // UPDATE VEHICLE
    // =====================================================

    @PutMapping("/{id}")
    @Operation(
            summary = "Update vehicle"
    )
    public VehicleResponse update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody
            VehicleRequest request) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.updateMyVehicle(
                userId,
                id,
                request
        );
    }

    // =====================================================
    // SET PRIMARY VEHICLE
    // =====================================================

    @PatchMapping("/{id}/primary")
    @Operation(
            summary = "Set vehicle as primary"
    )
    public VehicleResponse setPrimary(
            Authentication authentication,
            @PathVariable UUID id) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.setPrimaryVehicle(
                userId,
                id
        );
    }

    // =====================================================
    // DELETE VEHICLE
    // =====================================================

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Permanently delete vehicle"
    )
    public void delete(
            Authentication authentication,
            @PathVariable UUID id) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        service.deleteMyVehicle(
                userId,
                id
        );
    }
}