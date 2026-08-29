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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@Transactional
public class AuthService {

    private final AuthOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final MechanicRepository mechanicRepository;
    private final JwtService jwtService;
    private final TwoFactorService twoFactorService;

    private final int otpExpiryMinutes;
    private final int maxAttempts;
    private final int resendSeconds;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AuthService(
            AuthOtpRepository otpRepository,
            UserRepository userRepository,
            DriverRepository driverRepository,
            MechanicRepository mechanicRepository,
            JwtService jwtService,
            TwoFactorService twoFactorService,

            @Value("${truckassist.auth.otp.expiry-minutes:5}")
            int otpExpiryMinutes,

            @Value("${truckassist.auth.otp.max-attempts:5}")
            int maxAttempts,

            @Value("${truckassist.auth.otp.resend-seconds:60}")
            int resendSeconds) {

        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.mechanicRepository = mechanicRepository;
        this.jwtService = jwtService;
        this.twoFactorService = twoFactorService;

        this.otpExpiryMinutes = otpExpiryMinutes;
        this.maxAttempts = maxAttempts;
        this.resendSeconds = resendSeconds;
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
        // REQUEST OTP FROM 2FACTOR
        // =================================================

        String providerSessionId =
                twoFactorService.sendOtp(
                        phone
                );


        // =================================================
        // STORE 2FACTOR SESSION
        // =================================================

        AuthOtp authOtp =
                new AuthOtp();

        authOtp.setPhone(
                phone
        );

        authOtp.setProviderSessionId(
                providerSessionId
        );

        authOtp.setExpiresAt(
                now.plusMinutes(
                        otpExpiryMinutes
                )
        );

        authOtp.setAttempts(0);

        authOtp.setVerified(false);

        otpRepository.save(
                authOtp
        );


        // =================================================
        // RESPONSE
        // =================================================
        //
        // IMPORTANT:
        // We NEVER return the OTP.
        //
        // 2Factor generates the OTP and sends it
        // directly to the user's phone.
        // =================================================

        return new SendOtpResponse(
                true,
                "OTP sent successfully",
                otpExpiryMinutes * 60,
                null
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
        // VALIDATE OTP INPUT
        // =================================================

        if (otp == null ||
                otp.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "OTP is required"
            );
        }

        otp = otp.trim();


        // =================================================
        // FIND LATEST OTP SESSION
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


        // =================================================
        // CHECK EXPIRY
        // =================================================

        OffsetDateTime now =
                OffsetDateTime.now();

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
        // VERIFY OTP THROUGH 2FACTOR
        // =================================================

        boolean verified =
                twoFactorService.verifyOtp(
                        authOtp.getProviderSessionId(),
                        otp
                );

        if (!verified) {

            otpRepository.save(
                    authOtp
            );

            throw new IllegalArgumentException(
                    "Invalid OTP"
            );
        }


        // =================================================
        // OTP VERIFIED
        // =================================================

        authOtp.setVerified(
                true
        );

        authOtp.setVerifiedAt(
                now
        );

        otpRepository.save(
                authOtp
        );


        // =================================================
        // FIND OR CREATE USER
        // =================================================

        User user =
                userRepository
                        .findByPhone(phone)
                        .orElse(null);

        boolean isNewUser =
                false;


        // =================================================
        // NEW USER
        // =================================================

        if (user == null) {

            user =
                    new User();

            user.setPhone(
                    phone
            );

            user.setRole(
                    requestedRole
            );

            user.setStatus(
                    "ACTIVE"
            );

            user =
                    userRepository.save(
                            user
                    );

            isNewUser =
                    true;

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
                        userRepository.save(
                                user
                        );
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