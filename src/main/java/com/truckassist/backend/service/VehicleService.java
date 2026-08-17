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

        Driver driver = getDriverByUserId(userId);

        validateRegistrationNumber(
                request.registrationNumber()
        );

        Vehicle vehicle = new Vehicle();

        vehicle.setDriver(driver);

        vehicle.setRegistrationNumber(
                request.registrationNumber()
                        .trim()
                        .toUpperCase()
        );

        vehicle.setManufacturer(
                request.manufacturer()
        );

        vehicle.setModel(
                request.model()
        );

        vehicle.setVehicleType(
                request.vehicleType()
        );

        vehicle.setManufacturingYear(
                request.manufacturingYear()
        );

        vehicle.setColor(
                request.color()
        );

        vehicle.setPrimary(
                request.primary() != null &&
                        request.primary()
        );

        vehicle.setStatus("ACTIVE");

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

        Driver driver = getDriverByUserId(userId);

        return vehicleRepository
                .findByDriverId(driver.getId())
                .stream()
                .filter(vehicle ->
                        !"DELETED".equalsIgnoreCase(
                                vehicle.getStatus()
                        )
                )
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // GET VEHICLE
    // =====================================================

    @Transactional(readOnly = true)
    public VehicleResponse getById(UUID id) {

        Vehicle vehicle =
                vehicleRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle not found: " + id
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

        if (!vehicle.getDriver()
                .getId()
                .equals(driver.getId())) {

            throw new IllegalArgumentException(
                    "Vehicle does not belong to current driver"
            );
        }

        vehicle.setManufacturer(
                request.manufacturer()
        );

        vehicle.setModel(
                request.model()
        );

        vehicle.setVehicleType(
                request.vehicleType()
        );

        vehicle.setManufacturingYear(
                request.manufacturingYear()
        );

        vehicle.setColor(
                request.color()
        );

        if (request.primary() != null) {

            vehicle.setPrimary(
                    request.primary()
            );
        }

        return toResponse(
                vehicleRepository.save(vehicle)
        );
    }

    // =====================================================
    // DELETE CURRENT DRIVER VEHICLE
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

        if (!vehicle.getDriver()
                .getId()
                .equals(driver.getId())) {

            throw new IllegalArgumentException(
                    "Vehicle does not belong to current driver"
            );
        }

        // Soft delete
        vehicle.setStatus("DELETED");

        vehicleRepository.save(vehicle);
    }

    // =====================================================
    // EXISTING GET BY DRIVER
    // =====================================================

    @Transactional(readOnly = true)
    public List<VehicleResponse> getByDriver(
            UUID driverId) {

        return vehicleRepository
                .findByDriverId(driverId)
                .stream()
                .filter(vehicle ->
                        !"DELETED".equalsIgnoreCase(
                                vehicle.getStatus()
                        )
                )
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private Driver getDriverByUserId(UUID userId) {

        return driverRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver profile not found"
                        )
                );
    }

    private void validateRegistrationNumber(
            String registrationNumber) {

        if (registrationNumber == null ||
                registrationNumber.isBlank()) {

            throw new IllegalArgumentException(
                    "Vehicle registration number is required"
            );
        }

        String normalized =
                registrationNumber
                        .trim()
                        .toUpperCase();

        if (vehicleRepository
                .existsByRegistrationNumber(normalized)) {

            throw new IllegalArgumentException(
                    "Vehicle registration already exists"
            );
        }
    }

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