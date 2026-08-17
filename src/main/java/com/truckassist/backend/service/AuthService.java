package com.truckassist.backend.service;

import com.truckassist.backend.dto.auth.AuthResponse;
import com.truckassist.backend.dto.auth.SendOtpResponse;
import com.truckassist.backend.entity.AuthOtp;
import com.truckassist.backend.entity.Driver;
import com.truckassist.backend.entity.Mechanic;
import com.truckassist.backend.entity.User;
import com.truckassist.backend.repository.AuthOtpRepository;
import com.truckassist.backend.repository.DriverRepository;
import com.truckassist.backend.repository.MechanicRepository;
import com.truckassist.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Random;

@Service
@Transactional
public class AuthService {

    private final AuthOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final MechanicRepository mechanicRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final int otpExpiryMinutes;
    private final int maxAttempts;
    private final int resendSeconds;
    private final boolean devMode;

    private final Random random = new Random();

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AuthService(
            AuthOtpRepository otpRepository,
            UserRepository userRepository,
            DriverRepository driverRepository,
            MechanicRepository mechanicRepository,
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
        this.driverRepository = driverRepository;
        this.mechanicRepository = mechanicRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;

        this.otpExpiryMinutes = otpExpiryMinutes;
        this.maxAttempts = maxAttempts;
        this.resendSeconds = resendSeconds;
        this.devMode = devMode;
    }

    // =====================================================
    // DRIVER - SEND OTP
    // =====================================================

    public SendOtpResponse sendOtp(String phone) {

        return sendOtp(
                phone,
                "DRIVER"
        );
    }

    // =====================================================
    // ROLE-AWARE SEND OTP
    // =====================================================

    public SendOtpResponse sendOtp(
            String phone,
            String role) {

        phone = normalizePhone(phone);
        role = normalizeRole(role);

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
                    "Role: " + role
            );

            System.out.println(
                    "Phone: " + phone
            );

            System.out.println(
                    "OTP: " + otp
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
    // DRIVER - VERIFY OTP
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
    // ROLE-AWARE VERIFY OTP
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
                authOtp.getExpiresAt())) {

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

        // =================================================
        // INCREMENT ATTEMPT
        // =================================================

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

        // =================================================
        // NEW USER
        // =================================================

        if (user == null) {

            user =
                    new User();

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

            System.out.println(
                    "New "
                            + requestedRole
                            + " user created: "
                            + user.getId()
            );
        }

        // =================================================
        // EXISTING USER
        // =================================================

        else {

            String existingRole =
                    normalizeRole(
                            user.getRole()
                    );

            // -------------------------------------------------
            // Do not silently change account type
            // -------------------------------------------------

            if (!existingRole.equals(
                    requestedRole)) {

                throw new IllegalStateException(
                        "This mobile number is already registered as "
                                + existingRole
                                + ". Please use the correct application."
                );
            }

            // -------------------------------------------------
            // Ensure active
            // -------------------------------------------------

            if (user.getStatus() == null ||
                    !"ACTIVE".equalsIgnoreCase(
                            user.getStatus())) {

                user.setStatus(
                        "ACTIVE"
                );

                user =
                        userRepository.save(user);
            }
        }

        // =====================================================
        // ENSURE DRIVER PROFILE
        // =====================================================

        if ("DRIVER".equals(
                requestedRole)) {

            ensureDriverProfile(
                    user
            );
        }

        // =====================================================
        // ENSURE MECHANIC PROFILE
        // =====================================================

        else if ("MECHANIC".equals(
                requestedRole)) {

            ensureMechanicProfile(
                    user
            );
        }

        // =====================================================
        // GENERATE JWT
        // =====================================================

        String token =
                jwtService.generateToken(
                        user
                );

        // =====================================================
        // AUTH RESPONSE
        // =====================================================

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
    // ENSURE DRIVER PROFILE
    // =====================================================

    private void ensureDriverProfile(
            User user) {

        // Your existing DriverRepository provides
        // findByUserId(), not existsByUserId().

        if (driverRepository
                .findByUserId(
                        user.getId()
                )
                .isPresent()) {

            return;
        }

        // =================================================
        // CREATE DRIVER
        // =================================================

        Driver driver =
                new Driver();

        driver.setUser(
                user
        );

        // Profile details will be completed
        // from Driver Profile screen.

        driver.setLicenseNumber(
                null
        );

        driver.setLicenseExpiryDate(
                null
        );

        driver.setEmergencyContactName(
                null
        );

        driver.setEmergencyContactPhone(
                null
        );

        driver.setAvailable(
                true
        );

        driverRepository.save(
                driver
        );

        System.out.println(
                "Driver profile created for user: "
                        + user.getId()
        );
    }

    // =====================================================
    // ENSURE MECHANIC PROFILE
    // =====================================================

    private void ensureMechanicProfile(
            User user) {

        // MechanicRepository already provides
        // existsByUserId().

        if (mechanicRepository
                .existsByUserId(
                        user.getId()
                )) {

            return;
        }

        // =================================================
        // CREATE MECHANIC
        // =================================================

        Mechanic mechanic =
                new Mechanic();

        mechanic.setUser(
                user
        );

        // =================================================
        // INITIAL PROFILE
        // =================================================

        mechanic.setExperienceYears(
                null
        );

        mechanic.setWorkshopName(
                null
        );

        mechanic.setWorkshopAddress(
                null
        );

        // New mechanic starts offline
        mechanic.setAvailable(
                false
        );

        mechanic.setRating(
                BigDecimal.ZERO
        );

        mechanic.setTotalJobs(
                0
        );

        mechanic.setLatitude(
                null
        );

        mechanic.setLongitude(
                null
        );

        mechanic.setLastLocationAt(
                null
        );

        mechanicRepository.save(
                mechanic
        );

        System.out.println(
                "Mechanic profile created for user: "
                        + user.getId()
        );
    }

    // =====================================================
    // GENERATE OTP
    // =====================================================

    private String generateOtp() {

        return String.format(
                "%06d",
                random.nextInt(
                        1_000_000
                )
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

        String normalized =
                phone
                        .replaceAll(
                                "[\\s-]",
                                ""
                        )
                        .trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "Phone number is required"
            );
        }

        return normalized;
    }

    // =====================================================
    // NORMALIZE ROLE
    // =====================================================

    private String normalizeRole(
            String role) {

        if (role == null ||
                role.trim().isEmpty()) {

            return "DRIVER";
        }

        String normalized =
                role
                        .trim()
                        .toUpperCase();

        if (!"DRIVER".equals(
                normalized) &&
                !"MECHANIC".equals(
                        normalized)) {

            throw new IllegalArgumentException(
                    "Invalid authentication role: "
                            + role
            );
        }

        return normalized;
    }
}