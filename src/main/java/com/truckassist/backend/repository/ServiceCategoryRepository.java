package com.truckassist.backend.repository;

import com.truckassist.backend.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceCategoryRepository
        extends JpaRepository<ServiceCategory, UUID> {

    List<ServiceCategory> findByActiveTrueOrderByDisplayOrderAsc();
}