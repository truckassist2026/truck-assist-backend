package com.truckassist.backend.repository;

import com.truckassist.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    Optional<Payment>
    findByServiceRequestId(UUID serviceRequestId);
}