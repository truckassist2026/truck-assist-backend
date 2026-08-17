package com.truckassist.backend.controller;

import com.truckassist.backend.dto.auth.*;
import com.truckassist.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-otp")
    @Operation(
            summary = "Send OTP",
            description = "Send OTP to a mobile number"
    )
    public SendOtpResponse sendOtp(
            @Valid @RequestBody SendOtpRequest request) {

        return authService.sendOtp(
                request.phone()
        );
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Verify OTP",
            description = "Verify OTP and return JWT"
    )
    public AuthResponse verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        return authService.verifyOtp(
                request.phone(),
                request.otp()
        );
    }
}