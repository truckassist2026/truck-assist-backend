package com.truckassist.backend.service;

import com.truckassist.backend.dto.auth.AuthResponse;
import com.truckassist.backend.dto.auth.SendOtpResponse;
import com.truckassist.backend.entity.AuthOtp;
import com.truckassist.backend.entity.User;
import com.truckassist.backend.repository.AuthOtpRepository;
import com.truckassist.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Random;

@Service
@Transactional
public class AuthService {

    private final AuthOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final int otpExpiryMinutes;
    private final int maxAttempts;
    private final int resendSeconds;
    private final boolean devMode;

    private final Random random = new Random();

    public AuthService(
            AuthOtpRepository otpRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,

            @Value("${truckassist.auth.otp.expiry-minutes:5}")
            int otpExpiryMinutes,

            @Value("${truckassist.auth.otp.max-attempts:5}")
            int maxAttempts,

            @Value("${truckassist.auth.otp.resend-seconds:60}")
            int resendSeconds,

            @Value("${truckassist.auth.otp.dev-mode:false}")
            boolean devMode) {

        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;

        this.otpExpiryMinutes = otpExpiryMinutes;
        this.maxAttempts = maxAttempts;
        this.resendSeconds = resendSeconds;
        this.devMode = devMode;
    }

    // =====================================================
    // SEND OTP - DRIVER
    // =====================================================

    public SendOtpResponse sendOtp(String phone) {

        return sendOtp(phone, "DRIVER");
    }

    // =====================================================
    // SEND OTP - ROLE AWARE
    // =====================================================

    public SendOtpResponse sendOtp(
            String phone,
            String requestedRole) {

        phone = normalizePhone(phone);

        requestedRole =
                normalizeRole(requestedRole);

        /*
         * -------------------------------------------------
         * CHECK EXISTING USER
         * -------------------------------------------------
         *
         * We do this BEFORE sending OTP.
         *
         * Example:
         *
         * Existing DRIVER tries Mechanic app
         *       -> reject
         *
         * Existing MECHANIC tries Driver app
         *       -> reject
         */
        User existingUser =
                userRepository
                        .findByPhone(phone)
                        .orElse(null);

        if (existingUser != null) {

            validateExistingUserRole(
                    existingUser,
                    requestedRole
            );
        }

        OffsetDateTime now =
                OffsetDateTime.now();

        // =================================================
        // CHECK RESEND LIMIT
        // =================================================

        var previousOtp =
                otpRepository
                        .findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(
                                phone
                        );

        if (previousOtp.isPresent()) {

            AuthOtp previous =
                    previousOtp.get();

            long secondsSinceLastOtp =
                    Duration.between(
                            previous.getCreatedAt(),
                            now
                    ).getSeconds();

            if (secondsSinceLastOtp < resendSeconds) {

                long remaining =
                        resendSeconds -
                        secondsSinceLastOtp;

                throw new IllegalStateException(
                        "Please wait "
                                + remaining
                                + " seconds before requesting another OTP"
                );
            }
        }

        // =================================================
        // GENERATE OTP
        // =================================================

        String otp =
                generateOtp();

        AuthOtp authOtp =
                new AuthOtp();

        authOtp.setPhone(phone);

        // Never store actual OTP
        authOtp.setOtpHash(
                passwordEncoder.encode(otp)
        );

        authOtp.setExpiresAt(
                now.plusMinutes(
                        otpExpiryMinutes
                )
        );

        authOtp.setAttempts(0);

        authOtp.setVerified(false);

        otpRepository.save(authOtp);

        // =================================================
        // DEVELOPMENT MODE
        // =================================================

        if (devMode) {

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "TRUCK ASSIST DEVELOPMENT OTP"
            );

            System.out.println(
                    "Application Role: "
                            + requestedRole
            );

            System.out.println(
                    "Phone: "
                            + phone
            );

            System.out.println(
                    "OTP: "
                            + otp
            );

            System.out.println(
                    "========================================"
            );
        }

        return new SendOtpResponse(
                true,
                "OTP sent successfully",
                otpExpiryMinutes * 60,
                devMode ? otp : null
        );
    }

    // =====================================================
    // VERIFY OTP - DRIVER
    // =====================================================

    public AuthResponse verifyOtp(
            String phone,
            String otp) {

        return verifyOtp(
                phone,
                otp,
                "DRIVER"
        );
    }

    // =====================================================
    // VERIFY OTP - ROLE AWARE
    // =====================================================

    public AuthResponse verifyOtp(
            String phone,
            String otp,
            String requestedRole) {

        phone =
                normalizePhone(phone);

        requestedRole =
                normalizeRole(requestedRole);

        // =================================================
        // FIND OTP
        // =================================================

        AuthOtp authOtp =
                otpRepository
                        .findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(
                                phone
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "OTP not found or already used"
                                )
                        );

        OffsetDateTime now =
                OffsetDateTime.now();

        // =================================================
        // CHECK EXPIRY
        // =================================================

        if (now.isAfter(
                authOtp.getExpiresAt()
        )) {

            throw new IllegalArgumentException(
                    "OTP has expired"
            );
        }

        // =================================================
        // CHECK MAX ATTEMPTS
        // =================================================

        if (authOtp.getAttempts()
                >= maxAttempts) {

            throw new IllegalArgumentException(
                    "Maximum OTP attempts exceeded"
            );
        }

        // Increment attempts
        authOtp.setAttempts(
                authOtp.getAttempts() + 1
        );

        // =================================================
        // VERIFY OTP
        // =================================================

        if (!passwordEncoder.matches(
                otp,
                authOtp.getOtpHash())) {

            otpRepository.save(authOtp);

            throw new IllegalArgumentException(
                    "Invalid OTP"
            );
        }

        // =================================================
        // OTP VERIFIED
        // =================================================

        authOtp.setVerified(true);

        authOtp.setVerifiedAt(now);

        otpRepository.save(authOtp);

        // =================================================
        // FIND OR CREATE USER
        // =================================================

        User user =
                userRepository
                        .findByPhone(phone)
                        .orElse(null);

        boolean isNewUser = false;

        if (user == null) {

            /*
             * ------------------------------------------------
             * NEW USER
             * ------------------------------------------------
             *
             * The role now comes from the application.
             *
             * Driver app:
             *     DRIVER
             *
             * Mechanic app:
             *     MECHANIC
             */
            user = new User();

            user.setPhone(phone);

            user.setRole(
                    requestedRole
            );

            user.setStatus(
                    "ACTIVE"
            );

            user =
                    userRepository.save(user);

            isNewUser = true;

        } else {

            /*
             * ------------------------------------------------
             * EXISTING USER
             * ------------------------------------------------
             *
             * Do NOT change the existing user's role.
             *
             * Driver cannot login through Mechanic app.
             * Mechanic cannot login through Driver app.
             */
            validateExistingUserRole(
                    user,
                    requestedRole
            );
        }

        // =================================================
        // GENERATE JWT
        // =================================================

        String token =
                jwtService.generateToken(user);

        // =================================================
        // RETURN AUTH RESPONSE
        // =================================================

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirySeconds(),
                user.getId(),
                user.getRole(),
                isNewUser
        );
    }

    // =====================================================
    // VALIDATE EXISTING USER ROLE
    // =====================================================

    private void validateExistingUserRole(
            User user,
            String requestedRole) {

        String existingRole =
                user.getRole();

        if (existingRole == null ||
                existingRole.trim().isEmpty()) {

            throw new IllegalStateException(
                    "User role is not configured"
            );
        }

        if (!existingRole.equalsIgnoreCase(
                requestedRole
        )) {

            if ("DRIVER".equalsIgnoreCase(
                    existingRole
            )) {

                throw new IllegalStateException(
                        "This mobile number is registered as a Driver. Please use the Driver application."
                );
            }

            if ("MECHANIC".equalsIgnoreCase(
                    existingRole
            )) {

                throw new IllegalStateException(
                        "This mobile number is registered as a Mechanic. Please use the Mechanic application."
                );
            }

            throw new IllegalStateException(
                    "This mobile number is registered with another application."
            );
        }
    }

    // =====================================================
    // NORMALIZE ROLE
    // =====================================================

    private String normalizeRole(
            String role) {

        if (role == null ||
                role.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Application role is required"
            );
        }

        String normalized =
                role.trim()
                        .toUpperCase();

        if (!normalized.equals("DRIVER") &&
                !normalized.equals("MECHANIC")) {

            throw new IllegalArgumentException(
                    "Invalid application role"
            );
        }

        return normalized;
    }

    // =====================================================
    // GENERATE OTP
    // =====================================================

    private String generateOtp() {

        return String.format(
                "%06d",
                random.nextInt(1_000_000)
        );
    }

    // =====================================================
    // NORMALIZE PHONE
    // =====================================================

    private String normalizePhone(
            String phone) {

        if (phone == null) {

            throw new IllegalArgumentException(
                    "Phone number is required"
            );
        }

        return phone
                .replaceAll("[\\s-]", "")
                .trim();
    }
}