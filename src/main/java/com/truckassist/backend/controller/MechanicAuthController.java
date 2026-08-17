package com.truckassist.backend.controller;

import com.truckassist.backend.dto.auth.AuthResponse;
import com.truckassist.backend.dto.auth.SendOtpResponse;
import com.truckassist.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/mechanic")
public class MechanicAuthController {

    private final AuthService authService;

    public MechanicAuthController(
            AuthService authService) {

        this.authService = authService;
    }

    // =====================================================
    // SEND MECHANIC OTP
    // =====================================================

    @PostMapping("/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(
            @RequestBody SendOtpRequest request) {

        SendOtpResponse response =
                authService.sendOtp(
                        request.phone(),
                        "MECHANIC"
                );

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // VERIFY MECHANIC OTP
    // =====================================================

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @RequestBody VerifyOtpRequest request) {

        AuthResponse response =
                authService.verifyOtp(
                        request.phone(),
                        request.otp(),
                        "MECHANIC"
                );

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // REQUEST DTOs
    // =====================================================

    public record SendOtpRequest(
            String phone
    ) {
    }

    public record VerifyOtpRequest(
            String phone,
            String otp
    ) {
    }
}