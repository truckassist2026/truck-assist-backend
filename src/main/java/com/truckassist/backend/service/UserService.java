package com.truckassist.backend.service;

import com.truckassist.backend.dto.UserRequest;
import com.truckassist.backend.dto.UserResponse;
import com.truckassist.backend.entity.User;
import com.truckassist.backend.exception.ResourceNotFoundException;
import com.truckassist.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse create(UserRequest request) {

        if (userRepository.existsByPhone(request.phone())) {
            throw new IllegalArgumentException(
                    "User with this phone number already exists"
            );
        }

        User user = new User();

        user.setPhone(request.phone());
        user.setName(request.name());
        user.setEmail(request.email());

        user.setRole(
                request.role() == null || request.role().isBlank()
                        ? "DRIVER"
                        : request.role().toUpperCase()
        );

        user.setStatus("ACTIVE");

        User saved = userRepository.save(user);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + id
                        )
                );

        return toResponse(user);
    }

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }
}