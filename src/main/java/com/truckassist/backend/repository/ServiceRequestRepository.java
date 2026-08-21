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

    List<ServiceRequest>
    findByDriverIdOrderByCreatedAtDesc(UUID driverId);

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

    // Searching requests available for acceptance.
    List<ServiceRequest>
    findByStatusOrderByCreatedAtAsc(String status);

    // Requests already assigned to a particular mechanic.
    // Used to keep accepted jobs visible after leaving the screen.
    List<ServiceRequest>
    findByAssignedMechanicIdOrderByUpdatedAtDesc(
            UUID mechanicId
    );

    // COMPLETED is intentionally excluded from active-job checking.
    boolean
    existsByAssignedMechanicIdAndStatusIn(
            UUID mechanicId,
            List<String> statuses
    );

    // Atomic SEARCHING -> ASSIGNED assignment.
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
            @Param("requestId") UUID requestId,
            @Param("mechanicId") UUID mechanicId
    );
}
