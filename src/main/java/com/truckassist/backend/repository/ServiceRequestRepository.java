package com.truckassist.backend.repository;

import com.truckassist.backend.entity.ServiceRequest;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, UUID> {

    // =====================================================
    // DRIVER REQUESTS
    // =====================================================

    List<ServiceRequest>
    findByDriverIdOrderByCreatedAtDesc(
            UUID driverId
    );

    Optional<ServiceRequest>
    findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(
            UUID driverId,
            List<String> statuses
    );

    List<ServiceRequest>
    findByDriverIdAndStatusOrderByCreatedAtDesc(
            UUID driverId,
            String status
    );

    // =====================================================
    // MECHANIC - SEARCHING REQUESTS
    // =====================================================

    List<ServiceRequest>
    findByStatusOrderByCreatedAtAsc(
            String status
    );

    // =====================================================
    // MECHANIC - ASSIGNED REQUESTS
    //
    // Keeps the current mechanic's accepted request visible
    // after leaving the Active Request screen.
    // =====================================================

    List<ServiceRequest>
    findByAssignedMechanicIdOrderByUpdatedAtDesc(
            UUID mechanicId
    );

    // =====================================================
    // MECHANIC - ACTIVE REQUEST CHECK
    // =====================================================

    boolean
    existsByAssignedMechanicIdAndStatusIn(
            UUID mechanicId,
            List<String> statuses
    );

    // =====================================================
    // ATOMIC MECHANIC ASSIGNMENT
    //
    // SEARCHING -> ASSIGNED
    //
    // Only one mechanic can successfully update.
    // =====================================================

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Transactional
    @Query("""
        UPDATE ServiceRequest r
           SET r.assignedMechanicId = :mechanicId,
               r.status = 'ASSIGNED',
               r.updatedAt = CURRENT_TIMESTAMP
         WHERE r.id = :requestId
           AND r.status = 'SEARCHING'
           AND r.assignedMechanicId IS NULL
    """)
    int assignMechanic(
            @Param("requestId")
            UUID requestId,

            @Param("mechanicId")
            UUID mechanicId
    );
}
