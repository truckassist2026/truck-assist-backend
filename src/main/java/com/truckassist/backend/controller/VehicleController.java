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

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    // =====================================================
    // MY VEHICLES
    // =====================================================

    @GetMapping("/my")
    @Operation(summary = "Get current driver's vehicles")
    public List<VehicleResponse> getMyVehicles(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return service.getMyVehicles(userId);
    }

    // =====================================================
    // CREATE MY VEHICLE
    // =====================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create vehicle")
    public VehicleResponse create(
            Authentication authentication,
            @Valid @RequestBody VehicleRequest request) {

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
    // UPDATE MY VEHICLE
    // =====================================================

    @PutMapping("/{id}")
    @Operation(summary = "Update vehicle")
    public VehicleResponse update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody VehicleRequest request) {

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
    // DELETE MY VEHICLE
    // =====================================================

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete vehicle")
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

    // =====================================================
    // GET VEHICLE BY ID
    // =====================================================

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle by ID")
    public VehicleResponse get(
            @PathVariable UUID id) {

        return service.getById(id);
    }

    // =====================================================
    // EXISTING DRIVER VEHICLE API
    // =====================================================

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get vehicles by driver ID")
    public List<VehicleResponse> getByDriver(
            @PathVariable UUID driverId) {

        return service.getByDriver(driverId);
    }
}