package com.truckassist.backend.service;

import com.truckassist.backend.dto.MechanicAvailabilityRequest;
import com.truckassist.backend.dto.MechanicLocationRequest;
import com.truckassist.backend.dto.MechanicProfileRequest;
import com.truckassist.backend.dto.MechanicResponse;
import com.truckassist.backend.dto.NearbyMechanicResponse;
import com.truckassist.backend.entity.Mechanic;
import com.truckassist.backend.entity.User;
import com.truckassist.backend.exception.ResourceNotFoundException;
import com.truckassist.backend.repository.MechanicRepository;
import com.truckassist.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MechanicService {

    private final MechanicRepository mechanicRepository;
    private final UserRepository userRepository;

    public MechanicService(
            MechanicRepository mechanicRepository,
            UserRepository userRepository) {

        this.mechanicRepository = mechanicRepository;
        this.userRepository = userRepository;
    }


    // =====================================================
    // GET MY PROFILE
    // =====================================================

    @Transactional(readOnly = true)
    public MechanicResponse getMe(UUID userId) {

        Mechanic mechanic =
                getMechanicByUserId(userId);

        return toResponse(mechanic);
    }


    // =====================================================
    // CREATE / UPDATE MY PROFILE
    // =====================================================

    public MechanicResponse updateMe(
            UUID userId,
            MechanicProfileRequest request) {

        // =================================================
        // GET USER
        // =================================================

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );


        // =================================================
        // VALIDATE ROLE
        // =================================================

        if (!"MECHANIC".equalsIgnoreCase(
                user.getRole()
        )) {

            throw new IllegalArgumentException(
                    "User is not registered as a mechanic"
            );
        }


        // =================================================
        // UPDATE USER INFORMATION
        // =================================================
        //
        // Name and Email belong to the User entity.
        //
        // Phone is intentionally NOT changed here.
        // Phone is used for authentication.
        // =================================================

        if (request.name() != null) {

            String name =
                    request.name().trim();

            user.setName(
                    name.isEmpty()
                            ? null
                            : name
            );
        }


        if (request.email() != null) {

            String email =
                    request.email().trim();

            user.setEmail(
                    email.isEmpty()
                            ? null
                            : email
            );
        }


        // =================================================
        // SAVE USER
        // =================================================

        userRepository.save(user);


        // =================================================
        // GET EXISTING MECHANIC PROFILE
        // =================================================

        Mechanic mechanic =
                mechanicRepository
                        .findByUserId(userId)
                        .orElse(null);


        // =================================================
        // FIRST PROFILE CREATION
        // =================================================

        if (mechanic == null) {

            mechanic = new Mechanic();

            mechanic.setUser(user);

            mechanic.setAvailable(false);

            mechanic.setRating(
                    BigDecimal.ZERO
            );

            mechanic.setTotalJobs(0);
        }


        // =================================================
        // UPDATE MECHANIC INFORMATION
        // =================================================

        mechanic.setExperienceYears(
                request.experienceYears()
        );

        mechanic.setWorkshopName(
                request.workshopName()
        );

        mechanic.setWorkshopAddress(
                request.workshopAddress()
        );


        // =================================================
        // SAVE MECHANIC
        // =================================================

        Mechanic savedMechanic =
                mechanicRepository.save(
                        mechanic
                );


        // =================================================
        // RETURN UPDATED PROFILE
        // =================================================

        return toResponse(
                savedMechanic
        );
    }


    // =====================================================
    // AVAILABILITY
    // =====================================================

    public MechanicResponse updateAvailability(
            UUID userId,
            MechanicAvailabilityRequest request) {

        Mechanic mechanic =
                getMechanicByUserId(userId);

        boolean available =
                request.available();


        // A mechanic should not be available
        // without sending a location first.

        if (available &&
                (mechanic.getLatitude() == null ||
                 mechanic.getLongitude() == null)) {

            throw new IllegalStateException(
                    "Location is required before going online"
            );
        }


        mechanic.setAvailable(
                available
        );


        return toResponse(
                mechanicRepository.save(
                        mechanic
                )
        );
    }


    // =====================================================
    // LOCATION
    // =====================================================

    public MechanicResponse updateLocation(
            UUID userId,
            MechanicLocationRequest request) {

        Mechanic mechanic =
                getMechanicByUserId(userId);


        mechanic.setLatitude(
                request.latitude()
        );


        mechanic.setLongitude(
                request.longitude()
        );


        mechanic.setLastLocationAt(
                OffsetDateTime.now()
        );


        return toResponse(
                mechanicRepository.save(
                        mechanic
                )
        );
    }


    // =====================================================
    // NEARBY MECHANICS
    // =====================================================

    @Transactional(readOnly = true)
    public List<NearbyMechanicResponse> getNearbyMechanics(
            BigDecimal latitude,
            BigDecimal longitude,
            double radiusKm) {

        if (latitude == null ||
                longitude == null) {

            throw new IllegalArgumentException(
                    "Latitude and longitude are required"
            );
        }


        if (radiusKm <= 0) {

            radiusKm = 10;
        }


        final double searchRadius =
                radiusKm;


        OffsetDateTime locationCutoff =
                OffsetDateTime.now()
                        .minusMinutes(10);


        return mechanicRepository
                .findByAvailableTrue()
                .stream()

                // =================================================
                // MUST HAVE LOCATION
                // =================================================

                .filter(mechanic ->
                        mechanic.getLatitude() != null &&
                        mechanic.getLongitude() != null
                )

                // =================================================
                // LOCATION SHOULD BE FRESH
                // =================================================

                .filter(mechanic ->
                        mechanic.getLastLocationAt() != null &&
                        mechanic.getLastLocationAt()
                                .isAfter(locationCutoff)
                )

                // =================================================
                // CALCULATE DISTANCE
                // =================================================

                .map(mechanic -> {

                    double distance =
                            calculateDistanceKm(
                                    latitude.doubleValue(),
                                    longitude.doubleValue(),
                                    mechanic.getLatitude()
                                            .doubleValue(),
                                    mechanic.getLongitude()
                                            .doubleValue()
                            );


                    return new NearbyMechanicWithDistance(
                            mechanic,
                            distance
                    );
                })

                // =================================================
                // FILTER BY RADIUS
                // =================================================

                .filter(item ->
                        item.distanceKm()
                                <= searchRadius
                )

                // =================================================
                // SORT BY DISTANCE
                // =================================================

                .sorted(
                        Comparator.comparingDouble(
                                NearbyMechanicWithDistance
                                        ::distanceKm
                        )
                )

                // =================================================
                // RESPONSE
                // =================================================

                .map(item ->
                        toNearbyResponse(
                                item.mechanic(),
                                item.distanceKm()
                        )
                )

                .toList();
    }


    // =====================================================
    // GET MECHANIC
    // =====================================================

    private Mechanic getMechanicByUserId(
            UUID userId) {

        return mechanicRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mechanic profile not found"
                        )
                );
    }


    // =====================================================
    // RESPONSE
    // =====================================================

    private MechanicResponse toResponse(
            Mechanic mechanic) {

        User user =
                mechanic.getUser();


        return new MechanicResponse(

                mechanic.getId(),

                user.getId(),

                user.getName(),

                user.getPhone(),

                user.getEmail(),

                user.getProfileImageUrl(),

                mechanic.getExperienceYears(),

                mechanic.getWorkshopName(),

                mechanic.getWorkshopAddress(),

                mechanic.isAvailable(),

                mechanic.getRating(),

                mechanic.getTotalJobs(),

                mechanic.getLatitude(),

                mechanic.getLongitude(),

                mechanic.getLastLocationAt()
        );
    }


    // =====================================================
    // NEARBY RESPONSE
    // =====================================================

    private NearbyMechanicResponse toNearbyResponse(
            Mechanic mechanic,
            double distanceKm) {

        User user =
                mechanic.getUser();


        return new NearbyMechanicResponse(

                mechanic.getId(),

                user.getId(),

                user.getName(),

                user.getPhone(),

                mechanic.getWorkshopName(),

                mechanic.getRating(),

                mechanic.getTotalJobs(),

                mechanic.getLatitude(),

                mechanic.getLongitude(),

                Math.round(
                        distanceKm * 100.0
                ) / 100.0
        );
    }


    // =====================================================
    // HAVERSINE DISTANCE
    // =====================================================

    private double calculateDistanceKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        final double earthRadiusKm =
                6371.0;


        double dLat =
                Math.toRadians(
                        lat2 - lat1
                );


        double dLon =
                Math.toRadians(
                        lon2 - lon1
                );


        double a =
                Math.sin(dLat / 2)
                        * Math.sin(dLat / 2)

                +

                Math.cos(
                        Math.toRadians(lat1)
                )

                        * Math.cos(
                                Math.toRadians(lat2)
                        )

                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);


        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );


        return earthRadiusKm * c;
    }


    // =====================================================
    // INTERNAL RECORD
    // =====================================================

    private record NearbyMechanicWithDistance(
            Mechanic mechanic,
            double distanceKm
    ) {
    }
}