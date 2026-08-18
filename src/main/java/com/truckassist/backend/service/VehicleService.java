package com.truckassist.backend.service;

import com.truckassist.backend.dto.VehicleRequest;
import com.truckassist.backend.dto.VehicleResponse;
import com.truckassist.backend.entity.Driver;
import com.truckassist.backend.entity.Vehicle;
import com.truckassist.backend.exception.ResourceNotFoundException;
import com.truckassist.backend.repository.DriverRepository;
import com.truckassist.backend.repository.VehicleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository) {

        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    // =====================================================
    // CREATE VEHICLE FOR CURRENT DRIVER
    // =====================================================

    public VehicleResponse createForDriver(
            UUID userId,
            VehicleRequest request) {

        Driver driver =
                getDriverByUserId(userId);

        String registrationNumber =
                normalizeRegistrationNumber(
                        request.registrationNumber()
                );

        // -------------------------------------------------
        // Registration must be unique among existing
        // vehicles.
        // Since we are now permanently deleting vehicles,
        // a deleted vehicle will no longer exist.
        // -------------------------------------------------

        validateRegistrationNumberForCreate(
                registrationNumber
        );

        Vehicle vehicle =
                new Vehicle();

        vehicle.setDriver(driver);

        vehicle.setRegistrationNumber(
                registrationNumber
        );

        vehicle.setManufacturer(
                clean(request.manufacturer())
        );

        vehicle.setModel(
                clean(request.model())
        );

        vehicle.setVehicleType(
                clean(request.vehicleType())
        );

        vehicle.setManufacturingYear(
                request.manufacturingYear()
        );

        vehicle.setColor(
                clean(request.color())
        );

        vehicle.setStatus("ACTIVE");

        // -------------------------------------------------
        // Primary vehicle handling
        // -------------------------------------------------

        List<Vehicle> existingVehicles =
                getDriverVehicles(driver.getId());

        boolean requestedPrimary =
                Boolean.TRUE.equals(
                        request.primary()
                );

        if (existingVehicles.isEmpty()) {

            // First vehicle is automatically Primary
            vehicle.setPrimary(true);

        } else if (requestedPrimary) {

            // Clear existing Primary
            clearPrimaryVehicle(
                    driver.getId()
            );

            vehicle.setPrimary(true);

        } else {

            vehicle.setPrimary(false);
        }

        Vehicle saved =
                vehicleRepository.save(vehicle);

        return toResponse(saved);
    }

    // =====================================================
    // GET CURRENT DRIVER VEHICLES
    // =====================================================

    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(
            UUID userId) {

        Driver driver =
                getDriverByUserId(userId);

        return vehicleRepository
                .findByDriverId(driver.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // GET VEHICLE BY ID
    // =====================================================

    @Transactional(readOnly = true)
    public VehicleResponse getById(
            UUID id) {

        Vehicle vehicle =
                vehicleRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle not found: "
                                                + id
                                )
                        );

        return toResponse(vehicle);
    }

    // =====================================================
    // UPDATE CURRENT DRIVER VEHICLE
    // =====================================================

    public VehicleResponse updateMyVehicle(
            UUID userId,
            UUID vehicleId,
            VehicleRequest request) {

        Driver driver =
                getDriverByUserId(userId);

        Vehicle vehicle =
                vehicleRepository.findById(vehicleId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle not found: "
                                                + vehicleId
                                )
                        );

        // -------------------------------------------------
        // Ownership validation
        // -------------------------------------------------

        validateOwnership(
                vehicle,
                driver
        );

        // -------------------------------------------------
        // Registration number
        // -------------------------------------------------

        String registrationNumber =
                normalizeRegistrationNumber(
                        request.registrationNumber()
                );

        validateRegistrationNumberForUpdate(
                registrationNumber,
                vehicleId
        );

        vehicle.setRegistrationNumber(
                registrationNumber
        );

        // -------------------------------------------------
        // Vehicle information
        // -------------------------------------------------

        vehicle.setManufacturer(
                clean(request.manufacturer())
        );

        vehicle.setModel(
                clean(request.model())
        );

        vehicle.setVehicleType(
                clean(request.vehicleType())
        );

        vehicle.setManufacturingYear(
                request.manufacturingYear()
        );

        vehicle.setColor(
                clean(request.color())
        );

        // -------------------------------------------------
        // Primary
        // -------------------------------------------------

        if (Boolean.TRUE.equals(
                request.primary()
        )) {

            clearPrimaryVehicle(
                    driver.getId()
            );

            vehicle.setPrimary(true);

        } else if (Boolean.FALSE.equals(
                request.primary()
        )) {

            /*
             * If this is the only vehicle, keep it Primary.
             */

            List<Vehicle> vehicles =
                    getDriverVehicles(
                            driver.getId()
                    );

            if (vehicles.size() > 1) {

                vehicle.setPrimary(false);

            } else {

                vehicle.setPrimary(true);
            }
        }

        Vehicle saved =
                vehicleRepository.save(vehicle);

        return toResponse(saved);
    }

    // =====================================================
    // SET PRIMARY VEHICLE
    // =====================================================

    public VehicleResponse setPrimaryVehicle(
            UUID userId,
            UUID vehicleId) {

        Driver driver =
                getDriverByUserId(userId);

        Vehicle vehicle =
                vehicleRepository.findById(vehicleId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle not found: "
                                                + vehicleId
                                )
                        );

        // -------------------------------------------------
        // Ownership
        // -------------------------------------------------

        validateOwnership(
                vehicle,
                driver
        );

        // -------------------------------------------------
        // Clear existing Primary
        // -------------------------------------------------

        clearPrimaryVehicle(
                driver.getId()
        );

        // -------------------------------------------------
        // Set selected vehicle Primary
        // -------------------------------------------------

        vehicle.setPrimary(true);

        Vehicle saved =
                vehicleRepository.save(vehicle);

        return toResponse(saved);
    }

    // =====================================================
    // PERMANENT DELETE CURRENT DRIVER VEHICLE
    // =====================================================

    public void deleteMyVehicle(
            UUID userId,
            UUID vehicleId) {

        Driver driver =
                getDriverByUserId(userId);

        Vehicle vehicle =
                vehicleRepository.findById(vehicleId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle not found: "
                                                + vehicleId
                                )
                        );

        // -------------------------------------------------
        // Ownership
        // -------------------------------------------------

        validateOwnership(
                vehicle,
                driver
        );

        boolean wasPrimary =
                vehicle.isPrimary();

        // -------------------------------------------------
        // PERMANENT DELETE
        // -------------------------------------------------

        vehicleRepository.delete(vehicle);

        vehicleRepository.flush();

        // -------------------------------------------------
        // If Primary vehicle was deleted,
        // automatically select another vehicle.
        // -------------------------------------------------

        if (wasPrimary) {

            List<Vehicle> remainingVehicles =
                    getDriverVehicles(
                            driver.getId()
                    );

            if (!remainingVehicles.isEmpty()) {

                Vehicle newPrimary =
                        remainingVehicles.get(0);

                clearPrimaryVehicle(
                        driver.getId()
                );

                newPrimary.setPrimary(true);

                vehicleRepository.save(
                        newPrimary
                );
            }
        }
    }

    // =====================================================
    // GET VEHICLES BY DRIVER
    // =====================================================

    @Transactional(readOnly = true)
    public List<VehicleResponse> getByDriver(
            UUID driverId) {

        return vehicleRepository
                .findByDriverId(driverId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // GET DRIVER BY USER ID
    // =====================================================

    private Driver getDriverByUserId(
            UUID userId) {

        return driverRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver profile not found"
                        )
                );
    }

    // =====================================================
    // GET DRIVER VEHICLES
    // =====================================================

    private List<Vehicle> getDriverVehicles(
            UUID driverId) {

        return vehicleRepository
                .findByDriverId(driverId);
    }

    // =====================================================
    // CLEAR PRIMARY VEHICLE
    // =====================================================

    private void clearPrimaryVehicle(
            UUID driverId) {

        List<Vehicle> vehicles =
                vehicleRepository
                        .findByDriverId(driverId);

        for (Vehicle vehicle : vehicles) {

            if (vehicle.isPrimary()) {

                vehicle.setPrimary(false);

                vehicleRepository.save(vehicle);
            }
        }
    }

    // =====================================================
    // VALIDATE REGISTRATION - CREATE
    // =====================================================

    private void validateRegistrationNumberForCreate(
            String registrationNumber) {

        if (vehicleRepository
                .existsByRegistrationNumber(
                        registrationNumber
                )) {

            throw new IllegalArgumentException(
                    "Vehicle registration already exists"
            );
        }
    }

    // =====================================================
    // VALIDATE REGISTRATION - UPDATE
    // =====================================================

    private void validateRegistrationNumberForUpdate(
            String registrationNumber,
            UUID vehicleId) {

        boolean exists =
                vehicleRepository
                        .existsByRegistrationNumberAndIdNot(
                                registrationNumber,
                                vehicleId
                        );

        if (exists) {

            throw new IllegalArgumentException(
                    "Vehicle registration already exists"
            );
        }
    }

    // =====================================================
    // OWNERSHIP VALIDATION
    // =====================================================

    private void validateOwnership(
            Vehicle vehicle,
            Driver driver) {

        if (vehicle.getDriver() == null ||
                !vehicle.getDriver()
                        .getId()
                        .equals(driver.getId())) {

            throw new IllegalArgumentException(
                    "Vehicle does not belong to current driver"
            );
        }
    }

    // =====================================================
    // NORMALIZE REGISTRATION NUMBER
    // =====================================================

    private String normalizeRegistrationNumber(
            String registrationNumber) {

        if (registrationNumber == null ||
                registrationNumber.isBlank()) {

            throw new IllegalArgumentException(
                    "Vehicle registration number is required"
            );
        }

        return registrationNumber
                .trim()
                .toUpperCase();
    }

    // =====================================================
    // CLEAN OPTIONAL TEXT
    // =====================================================

    private String clean(String value) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        return cleaned;
    }

    // =====================================================
    // RESPONSE MAPPER
    // =====================================================

    private VehicleResponse toResponse(
            Vehicle vehicle) {

        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getDriver().getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getManufacturer(),
                vehicle.getModel(),
                vehicle.getVehicleType(),
                vehicle.getManufacturingYear(),
                vehicle.getColor(),
                vehicle.isPrimary(),
                vehicle.getStatus()
        );
    }
}