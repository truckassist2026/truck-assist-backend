package com.truckassist.backend.repository;

import com.truckassist.backend.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByDriverIdOrderByCreatedAtDesc(
            UUID driverId
    );

    Optional<ServiceRequest> findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(
            UUID driverId,
            List<String> statuses
    );

    List<ServiceRequest> findByDriverIdAndStatusOrderByCreatedAtDesc(
            UUID driverId,
            String status
    );
}