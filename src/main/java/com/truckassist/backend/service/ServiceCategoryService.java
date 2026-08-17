package com.truckassist.backend.service;

import com.truckassist.backend.entity.ServiceCategory;
import com.truckassist.backend.repository.ServiceCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServiceCategoryService {

    private final ServiceCategoryRepository repository;

    public ServiceCategoryService(
            ServiceCategoryRepository repository) {

        this.repository = repository;
    }

    public List<ServiceCategory> getActiveCategories() {

        return repository.findByActiveTrueOrderByDisplayOrderAsc();
    }
}