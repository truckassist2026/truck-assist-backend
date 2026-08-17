package com.truckassist.backend.controller;

import com.truckassist.backend.entity.ServiceCategory;
import com.truckassist.backend.service.ServiceCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-categories")
public class ServiceCategoryController {

    private final ServiceCategoryService service;

    public ServiceCategoryController(
            ServiceCategoryService service) {

        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get active service categories")
    public List<ServiceCategory> getAll() {

        return service.getActiveCategories();
    }
}