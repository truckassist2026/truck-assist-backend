package com.truckassist.backend.service;

import com.truckassist.backend.dto.MechanicOtpRequest;
import com.truckassist.backend.dto.MechanicVerifyOtpRequest;
import com.truckassist.backend.entity.Mechanic;
import com.truckassist.backend.entity.User;
import com.truckassist.backend.repository.MechanicRepository;
import com.truckassist.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class MechanicAuthService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final MechanicRepository mechanicRepository;

    public MechanicAuthService(
            AuthService authService,
            UserRepository userRepository,
            MechanicRepository mechanicRepository) {

        this.authService = authService;
        this.userRepository = userRepository;
        this.mechanicRepository = mechanicRepository;
    }

    /**
     * Sends OTP for Mechanic application.
     *
     * IMPORTANT:
     * The existing AuthService should remain the single
     * source for OTP generation/storage.
     */
    public Object sendOtp(
            MechanicOtpRequest request) {

        String phone =
                normalizePhone(request.phone());

        User existingUser =
                userRepository
                        .findByPhone(phone)
                        .orElse(null);

        if (existingUser != null) {

            String role =
                    existingUser.getRole();

            if ("DRIVER".equalsIgnoreCase(role)) {

                throw new IllegalStateException(
                        "This mobile number is registered as a DRIVER. Please use the Driver application."
                );
            }

            if (!"MECHANIC".equalsIgnoreCase(role)) {

                throw new IllegalStateException(
                        "This mobile number is not registered as a Mechanic."
                );
            }
        }

        /*
         * Reuse the existing OTP implementation.
         *
         * Your current AuthService already handles:
         * - OTP generation
         * - BCrypt/hash storage
         * - expiry
         * - attempts
         * - auth_otps persistence
         */
        return authService.sendOtp(phone);
    }

    /**
     * Verifies Mechanic OTP and creates the Mechanic account
     * when the phone number is new.
     */
    public Object verifyOtp(
            MechanicVerifyOtpRequest request) {

        String phone =
                normalizePhone(request.phone());

        User existingUser =
                userRepository
                        .findByPhone(phone)
                        .orElse(null);

        /*
         * Existing DRIVER must not be allowed into
         * the Mechanic application.
         */
        if (existingUser != null &&
                "DRIVER".equalsIgnoreCase(
                        existingUser.getRole()
                )) {

            throw new IllegalStateException(
                    "This mobile number is registered as a DRIVER. Please use the Driver application."
            );
        }

        /*
         * Existing non-mechanic roles are rejected.
         */
        if (existingUser != null &&
                !"MECHANIC".equalsIgnoreCase(
                        existingUser.getRole()
                )) {

            throw new IllegalStateException(
                    "This mobile number cannot be used for Mechanic login."
            );
        }

        /*
         * Let the existing AuthService perform the
         * actual OTP verification and JWT creation.
         *
         * This must return your existing authentication
         * response/user information.
         */
        Object authResponse =
                authService.verifyOtp(
                        phone,
                        request.otp()
                );

        /*
         * After successful OTP verification, make sure
         * the account has MECHANIC role.
         *
         * If this is a new account, the existing AuthService
         * must expose/create the user before this point.
         */
        User user =
                userRepository
                        .findByPhone(phone)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "User was not created after OTP verification"
                                )
                        );

        /*
         * Existing DRIVER protection again.
         */
        if ("DRIVER".equalsIgnoreCase(
                user.getRole()
        )) {

            throw new IllegalStateException(
                    "This mobile number is registered as a DRIVER. Please use the Driver application."
            );
        }

        /*
         * New user:
         * Change only newly created account to MECHANIC.
         *
         * We do NOT change an existing DRIVER.
         */
        if (!"MECHANIC".equalsIgnoreCase(
                user.getRole()
        )) {

            user.setRole("MECHANIC");
            user.setStatus("ACTIVE");

            user =
                    userRepository.save(user);
        }

        /*
         * Create Mechanic profile if it doesn't exist.
         */
        Mechanic mechanic =
                mechanicRepository
                        .findByUserId(user.getId())
                        .orElse(null);

        if (mechanic == null) {

            mechanic =
                    new Mechanic();

            mechanic.setUser(user);
            mechanic.setAvailable(false);

            mechanicRepository.save(
                    mechanic
            );
        }

        return authResponse;
    }

    private String normalizePhone(
            String phone) {

        if (phone == null) {
            throw new IllegalArgumentException(
                    "Phone number is required"
            );
        }

        String value =
                phone.trim()
                        .replace(" ", "");

        /*
         * Mobile app sends 10 digit Indian number.
         * Database currently stores +91 for OTP records,
         * so normalize to +91.
         */
        if (value.matches("^[0-9]{10}$")) {
            return "+91" + value;
        }

        return value;
    }
}