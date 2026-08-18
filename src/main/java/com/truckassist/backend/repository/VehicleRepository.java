package com.truckassist.backend.repository;

import com.truckassist.backend.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository
        extends JpaRepository<Vehicle, UUID> {

    // =====================================================
    // GET VEHICLES BY DRIVER
    // =====================================================

    List<Vehicle> findByDriverId(
            UUID driverId
    );

    // =====================================================
    // CREATE DUPLICATE CHECK
    // =====================================================

    boolean existsByRegistrationNumber(
            String registrationNumber
    );

    // =====================================================
    // UPDATE DUPLICATE CHECK
    // =====================================================

    boolean existsByRegistrationNumberAndIdNot(
            String registrationNumber,
            UUID id
    );
}