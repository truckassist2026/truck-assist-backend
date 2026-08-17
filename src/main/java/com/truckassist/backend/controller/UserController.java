package com.truckassist.backend.controller;

import com.truckassist.backend.dto.UserRequest;
import com.truckassist.backend.dto.UserResponse;
import com.truckassist.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user")
    public UserResponse create(
            @Valid @RequestBody UserRequest request) {

        return service.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user")
    public UserResponse get(@PathVariable UUID id) {

        return service.getById(id);
    }
}