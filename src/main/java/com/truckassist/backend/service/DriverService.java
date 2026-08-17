package com.truckassist.backend.service;

import com.truckassist.backend.dto.DriverRequest;
import com.truckassist.backend.dto.DriverResponse;
import com.truckassist.backend.entity.Driver;
import com.truckassist.backend.entity.User;
import com.truckassist.backend.exception.ResourceNotFoundException;
import com.truckassist.backend.repository.DriverRepository;
import com.truckassist.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    public DriverService(
            DriverRepository driverRepository,
            UserRepository userRepository) {

        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
    }

    // =====================================================
    // GET DRIVER BY ID
    // =====================================================

    @Transactional(readOnly = true)
    public DriverResponse getById(UUID id) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver not found: " + id
                        )
                );

        return toResponse(driver);
    }

    // =====================================================
    // GET DRIVER BY USER ID
    // =====================================================

    @Transactional(readOnly = true)
    public DriverResponse getByUserId(UUID userId) {

        Driver driver = driverRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver profile not found for user: "
                                        + userId
                        )
                );

        return toResponse(driver);
    }

    // =====================================================
    // CREATE DRIVER PROFILE
    // =====================================================

    public DriverResponse create(DriverRequest request) {

        User user = userRepository.findById(request.userId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: "
                                        + request.userId()
                        )
                );

        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {

            throw new IllegalArgumentException(
                    "User is not registered as a driver"
            );
        }

        driverRepository.findByUserId(request.userId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Driver profile already exists"
                    );
                });

        Driver driver = new Driver();

        driver.setUser(user);
        driver.setLicenseNumber(
                request.licenseNumber()
        );
        driver.setLicenseExpiryDate(
                request.licenseExpiryDate()
        );
        driver.setEmergencyContactName(
                request.emergencyContactName()
        );
        driver.setEmergencyContactPhone(
                request.emergencyContactPhone()
        );
        driver.setAvailable(true);

        Driver saved = driverRepository.save(driver);

        return toResponse(saved);
    }

    // =====================================================
    // UPDATE DRIVER PROFILE
    // =====================================================

    public DriverResponse updateByUserId(
            UUID userId,
            DriverRequest request) {

        Driver driver = driverRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver profile not found"
                        )
                );

        driver.setLicenseNumber(
                request.licenseNumber()
        );

        driver.setLicenseExpiryDate(
                request.licenseExpiryDate()
        );

        driver.setEmergencyContactName(
                request.emergencyContactName()
        );

        driver.setEmergencyContactPhone(
                request.emergencyContactPhone()
        );

        return toResponse(
                driverRepository.save(driver)
        );
    }

    // =====================================================
    // UPDATE DRIVER AVAILABILITY
    // =====================================================

    public DriverResponse updateAvailability(
            UUID userId,
            boolean available) {

        Driver driver = driverRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver profile not found"
                        )
                );

        driver.setAvailable(available);

        return toResponse(
                driverRepository.save(driver)
        );
    }

    // =====================================================
    // MAPPER
    // =====================================================

    private DriverResponse toResponse(Driver driver) {

        User user = driver.getUser();

        return new DriverResponse(
                driver.getId(),
                user.getId(),
                user.getName(),
                user.getPhone(),
                driver.getLicenseNumber(),
                driver.getLicenseExpiryDate(),
                driver.getEmergencyContactName(),
                driver.getEmergencyContactPhone(),
                driver.isAvailable()
        );
    }
}