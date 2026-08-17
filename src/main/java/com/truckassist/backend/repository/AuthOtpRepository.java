package com.truckassist.backend.repository;

import com.truckassist.backend.entity.AuthOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthOtpRepository
        extends JpaRepository<AuthOtp, UUID> {

    Optional<AuthOtp> findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(
            String phone
    );
}