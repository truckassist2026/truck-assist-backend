package com.truckassist.backend.repository;

import com.truckassist.backend.entity.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MechanicRepository
        extends JpaRepository<Mechanic, UUID> {

    Optional<Mechanic> findByUserId(UUID userId);

    List<Mechanic> findByAvailableTrue();

    boolean existsByUserId(UUID userId);
}