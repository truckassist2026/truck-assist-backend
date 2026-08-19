package com.truckassist.backend.service;

import com.truckassist.backend.dto.CreateServiceRequestRequest;
import com.truckassist.backend.dto.MechanicServiceRequestResponse;
import com.truckassist.backend.dto.RequestStatusHistoryResponse;
import com.truckassist.backend.dto.ServiceRequestResponse;

import com.truckassist.backend.entity.Driver;
import com.truckassist.backend.entity.Mechanic;
import com.truckassist.backend.entity.RequestStatusHistory;
import com.truckassist.backend.entity.ServiceRequest;
import com.truckassist.backend.entity.Vehicle;

import com.truckassist.backend.exception.ResourceNotFoundException;

import com.truckassist.backend.repository.DriverRepository;
import com.truckassist.backend.repository.MechanicRepository;
import com.truckassist.backend.repository.RequestStatusHistoryRepository;
import com.truckassist.backend.repository.ServiceRequestRepository;
import com.truckassist.backend.repository.VehicleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;


@Service
@Transactional
public class ServiceRequestService {


    private final ServiceRequestRepository requestRepository;

    private final RequestStatusHistoryRepository historyRepository;

    private final DriverRepository driverRepository;

    private final VehicleRepository vehicleRepository;

    private final MechanicRepository mechanicRepository;


    public ServiceRequestService(
            ServiceRequestRepository requestRepository,
            RequestStatusHistoryRepository historyRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            MechanicRepository mechanicRepository) {

        this.requestRepository =
                requestRepository;

        this.historyRepository =
                historyRepository;

        this.driverRepository =
                driverRepository;

        this.vehicleRepository =
                vehicleRepository;

        this.mechanicRepository =
                mechanicRepository;
    }


    // =====================================================
    // CREATE REQUEST
    // =====================================================

    public ServiceRequestResponse create(
            UUID userId,
            CreateServiceRequestRequest request) {


        // =================================================
        // GET DRIVER
        // =================================================

        Driver driver =
                driverRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver profile not found"
                                )
                        );


        // =================================================
        // GET VEHICLE
        // =================================================

        Vehicle vehicle =
                vehicleRepository
                        .findById(
                                request.vehicleId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle not found"
                                )
                        );


        // =================================================
        // VEHICLE OWNERSHIP
        // =================================================

        if (!vehicle.getDriver()
                .getId()
                .equals(driver.getId())) {

            throw new IllegalArgumentException(
                    "Vehicle does not belong to current driver"
            );
        }


        // =================================================
        // DRIVER ACTIVE REQUEST CHECK
        // =================================================

        if (hasActiveRequest(
                driver.getId()
        )) {

            throw new IllegalStateException(
                    "You already have an active service request"
            );
        }


        // =================================================
        // CATEGORY
        // =================================================

        String category =
                request.category()
                        .trim()
                        .toUpperCase();


        validateCategory(
                category
        );


        // =================================================
        // CREATE SERVICE REQUEST
        // =================================================

        ServiceRequest serviceRequest =
                new ServiceRequest();


        serviceRequest.setDriver(
                driver
        );


        serviceRequest.setVehicle(
                vehicle
        );


        serviceRequest.setCategory(
                category
        );


        serviceRequest.setDescription(
                request.description()
        );


        serviceRequest.setLatitude(
                request.latitude()
        );


        serviceRequest.setLongitude(
                request.longitude()
        );


        serviceRequest.setAddress(
                request.address()
        );


        serviceRequest.setStatus(
                "SEARCHING"
        );


        ServiceRequest saved =
                requestRepository.save(
                        serviceRequest
                );


        // =================================================
        // HISTORY: CREATED
        // =================================================

        addHistory(
                saved,
                "CREATED",
                userId,
                "Service request created"
        );


        // =================================================
        // HISTORY: SEARCHING
        // =================================================

        addHistory(
                saved,
                "SEARCHING",
                userId,
                "Searching for nearby mechanics"
        );


        return toResponse(
                saved
        );
    }


    // =====================================================
    // GET MY REQUESTS
    // =====================================================

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> getMyRequests(
            UUID userId) {


        Driver driver =
                driverRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver profile not found"
                                )
                        );


        return requestRepository
                .findByDriverIdOrderByCreatedAtDesc(
                        driver.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // =====================================================
    // GET ACTIVE REQUEST
    // =====================================================

    @Transactional(readOnly = true)
    public ServiceRequestResponse getActiveRequest(
            UUID userId) {


        Driver driver =
                driverRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver profile not found"
                                )
                        );


        List<String> activeStatuses =
                List.of(
                        "CREATED",
                        "SEARCHING",
                        "ASSIGNED",
                        "MECHANIC_EN_ROUTE",
                        "ARRIVED",
                        "IN_PROGRESS",
                        "PAYMENT_PENDING"
                );


        ServiceRequest request =
                requestRepository
                        .findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(
                                driver.getId(),
                                activeStatuses
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "No active service request"
                                )
                        );


        return toResponse(
                request
        );
    }


    // =====================================================
    // GET REQUEST BY ID - DRIVER
    // =====================================================

    @Transactional(readOnly = true)
    public ServiceRequestResponse getById(
            UUID userId,
            UUID requestId) {


        Driver driver =
                driverRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver profile not found"
                                )
                        );


        ServiceRequest request =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Service request not found"
                                )
                        );


        verifyOwnership(
                request,
                driver
        );


        return toResponse(
                request
        );
    }


    // =====================================================
    // GET HISTORY
    // =====================================================

    @Transactional(readOnly = true)
    public List<RequestStatusHistoryResponse> getHistory(
            UUID userId,
            UUID requestId) {


        Driver driver =
                driverRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver profile not found"
                                )
                        );


        ServiceRequest request =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Service request not found"
                                )
                        );


        verifyOwnership(
                request,
                driver
        );


        return historyRepository
                .findByRequestIdOrderByCreatedAtAsc(
                        requestId
                )
                .stream()
                .map(history ->
                        new RequestStatusHistoryResponse(

                                history.getId(),

                                history.getRequest()
                                        .getId(),

                                history.getStatus(),

                                history.getChangedByUserId(),

                                history.getNotes(),

                                history.getCreatedAt()
                        )
                )
                .toList();
    }


    // =====================================================
    // CANCEL REQUEST
    // =====================================================

    public ServiceRequestResponse cancel(
            UUID userId,
            UUID requestId) {


        Driver driver =
                driverRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver profile not found"
                                )
                        );


        ServiceRequest request =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Service request not found"
                                )
                        );


        verifyOwnership(
                request,
                driver
        );


        if (!isCancellable(
                request.getStatus()
        )) {

            throw new IllegalStateException(
                    "Request cannot be cancelled at status: "
                            + request.getStatus()
            );
        }


        request.setStatus(
                "CANCELLED"
        );


        request.setCancelledAt(
                OffsetDateTime.now()
        );


        ServiceRequest saved =
                requestRepository.save(
                        request
                );


        addHistory(
                saved,
                "CANCELLED",
                userId,
                "Cancelled by driver"
        );


        return toResponse(
                saved
        );
    }


    // =====================================================
    // GET AVAILABLE REQUESTS FOR MECHANIC
    // =====================================================

    @Transactional(readOnly = true)
    public List<MechanicServiceRequestResponse>
    getAvailableRequests(
            UUID userId) {


        // =================================================
        // GET CURRENT MECHANIC
        // =================================================

        Mechanic mechanic =
                mechanicRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Mechanic profile not found"
                                )
                        );


        // =================================================
        // MECHANIC MUST BE ONLINE
        // =================================================

        if (!mechanic.isAvailable()) {

            return List.of();
        }


        // =================================================
        // MECHANIC MUST HAVE LOCATION
        // =================================================

        if (
                mechanic.getLatitude() == null ||
                mechanic.getLongitude() == null
        ) {

            return List.of();
        }


        // =================================================
        // MECHANIC ACTIVE REQUEST CHECK
        // =================================================

        List<String> activeStatuses =
                List.of(
                        "ASSIGNED",
                        "MECHANIC_EN_ROUTE",
                        "ARRIVED",
                        "IN_PROGRESS",
                        "PAYMENT_PENDING"
                );


        boolean hasActiveRequest =
                requestRepository
                        .existsByAssignedMechanicIdAndStatusIn(
                                mechanic.getId(),
                                activeStatuses
                        );


        if (hasActiveRequest) {

            return List.of();
        }


        // =================================================
        // GET SEARCHING REQUESTS
        // =================================================

        List<ServiceRequest> requests =
                requestRepository
                        .findByStatusOrderByCreatedAtAsc(
                                "SEARCHING"
                        );


        final double mechanicLatitude =
                mechanic.getLatitude()
                        .doubleValue();


        final double mechanicLongitude =
                mechanic.getLongitude()
                        .doubleValue();


        // =================================================
        // SERVICE RADIUS
        // =================================================

        final double radiusKm =
                10.0;


        return requests
                .stream()


                // =================================================
                // REQUEST MUST HAVE LOCATION
                // =================================================

                .filter(request ->
                        request.getLatitude() != null &&
                        request.getLongitude() != null
                )


                // =================================================
                // CALCULATE DISTANCE
                // =================================================

                .map(request -> {

                    double distance =
                            calculateDistanceKm(

                                    mechanicLatitude,

                                    mechanicLongitude,

                                    request.getLatitude()
                                            .doubleValue(),

                                    request.getLongitude()
                                            .doubleValue()
                            );


                    return new RequestWithDistance(
                            request,
                            distance
                    );
                })


                // =================================================
                // FILTER 10 KM
                // =================================================

                .filter(item ->
                        item.distanceKm()
                                <= radiusKm
                )


                // =================================================
                // NEAREST FIRST
                // =================================================

                .sorted(
                        Comparator.comparingDouble(
                                RequestWithDistance
                                        ::distanceKm
                        )
                )


                // =================================================
                // RESPONSE
                // =================================================

                .map(item -> {

                    ServiceRequest request =
                            item.request();


                    return new MechanicServiceRequestResponse(

                            request.getId(),

                            request.getDriver()
                                    .getId(),

                            request.getVehicle()
                                    .getId(),

                            request.getCategory(),

                            request.getDescription(),

                            request.getLatitude(),

                            request.getLongitude(),

                            request.getAddress(),

                            request.getStatus(),

                            request.getCreatedAt(),

                            Math.round(
                                    item.distanceKm()
                                            * 100.0
                            ) / 100.0
                    );
                })


                .toList();
    }


    // =====================================================
    // GET SINGLE REQUEST FOR MECHANIC
    // =====================================================
    //
    // This endpoint is used by:
    //
    // GET
    // /api/v1/mechanics/requests/{requestId}
    //
    // =====================================================

    @Transactional(readOnly = true)
    public MechanicServiceRequestResponse
    getRequestByIdForMechanic(
            UUID userId,
            UUID requestId) {


        // =================================================
        // GET CURRENT MECHANIC
        // =================================================

        Mechanic mechanic =
                mechanicRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Mechanic profile not found"
                                )
                        );


        // =================================================
        // MECHANIC MUST BE ONLINE
        // =================================================

        if (!mechanic.isAvailable()) {

            throw new IllegalStateException(
                    "Mechanic is not available"
            );
        }


        // =================================================
        // MECHANIC MUST HAVE LOCATION
        // =================================================

        if (
                mechanic.getLatitude() == null ||
                mechanic.getLongitude() == null
        ) {

            throw new IllegalStateException(
                    "Mechanic location is required"
            );
        }


        // =================================================
        // GET REQUEST
        // =================================================

        ServiceRequest request =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Service request not found"
                                )
                        );


        // =================================================
        // REQUEST MUST STILL BE SEARCHING
        // =================================================

        if (!"SEARCHING".equals(
                request.getStatus()
        )) {

            throw new IllegalStateException(
                    "Service request is no longer available"
            );
        }


        // =================================================
        // REQUEST MUST HAVE LOCATION
        // =================================================

        if (
                request.getLatitude() == null ||
                request.getLongitude() == null
        ) {

            throw new IllegalStateException(
                    "Service request location is unavailable"
            );
        }


        // =================================================
        // CALCULATE DISTANCE
        // =================================================

        double distanceKm =
                calculateDistanceKm(

                        mechanic.getLatitude()
                                .doubleValue(),

                        mechanic.getLongitude()
                                .doubleValue(),

                        request.getLatitude()
                                .doubleValue(),

                        request.getLongitude()
                                .doubleValue()
                );


        // =================================================
        // 10 KM SERVICE RADIUS
        // =================================================

        if (distanceKm > 10.0) {

            throw new IllegalStateException(
                    "Service request is outside the service radius"
            );
        }


        // =================================================
        // RETURN REQUEST DETAILS
        // =================================================

        return new MechanicServiceRequestResponse(

                request.getId(),

                request.getDriver()
                        .getId(),

                request.getVehicle()
                        .getId(),

                request.getCategory(),

                request.getDescription(),

                request.getLatitude(),

                request.getLongitude(),

                request.getAddress(),

                request.getStatus(),

                request.getCreatedAt(),

                Math.round(
                        distanceKm * 100.0
                ) / 100.0
        );
    }


    // =====================================================
    // MECHANIC ACCEPT REQUEST
    // =====================================================

    public ServiceRequestResponse acceptRequest(
            UUID userId,
            UUID requestId) {


        // =================================================
        // GET MECHANIC
        // =================================================

        Mechanic mechanic =
                mechanicRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Mechanic profile not found"
                                )
                        );


        // =================================================
        // MECHANIC MUST BE AVAILABLE
        // =================================================

        if (!mechanic.isAvailable()) {

            throw new IllegalStateException(
                    "Mechanic is not available"
            );
        }


        // =================================================
        // MECHANIC MUST HAVE LOCATION
        // =================================================

        if (
                mechanic.getLatitude() == null ||
                mechanic.getLongitude() == null
        ) {

            throw new IllegalStateException(
                    "Mechanic location is required"
            );
        }


        // =================================================
        // MECHANIC ACTIVE REQUEST CHECK
        // =================================================

        List<String> activeStatuses =
                List.of(
                        "ASSIGNED",
                        "MECHANIC_EN_ROUTE",
                        "ARRIVED",
                        "IN_PROGRESS",
                        "PAYMENT_PENDING"
                );


        boolean hasActiveRequest =
                requestRepository
                        .existsByAssignedMechanicIdAndStatusIn(
                                mechanic.getId(),
                                activeStatuses
                        );


        if (hasActiveRequest) {

            throw new IllegalStateException(
                    "Mechanic already has an active service request"
            );
        }


        // =================================================
        // VERIFY REQUEST EXISTS
        // =================================================

        ServiceRequest request =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Service request not found"
                                )
                        );


        // =================================================
        // REQUEST MUST BE SEARCHING
        // =================================================

        if (!"SEARCHING".equals(
                request.getStatus()
        )) {

            throw new IllegalStateException(
                    "Service request is no longer available"
            );
        }


        // =================================================
        // REQUEST MUST NOT ALREADY BE ASSIGNED
        // =================================================

        if (
                request.getAssignedMechanicId()
                        != null
        ) {

            throw new IllegalStateException(
                    "Service request is already assigned"
            );
        }


        // =================================================
        // DISTANCE VALIDATION
        // =================================================

        if (
                request.getLatitude() != null &&
                request.getLongitude() != null
        ) {

            double distanceKm =
                    calculateDistanceKm(

                            mechanic.getLatitude()
                                    .doubleValue(),

                            mechanic.getLongitude()
                                    .doubleValue(),

                            request.getLatitude()
                                    .doubleValue(),

                            request.getLongitude()
                                    .doubleValue()
                    );


            if (distanceKm > 10.0) {

                throw new IllegalStateException(
                        "Service request is outside the service radius"
                );
            }
        }


        // =================================================
        // ATOMIC ASSIGNMENT
        //
        // SEARCHING -> ASSIGNED
        //
        // Only one mechanic can win.
        // =================================================

        int updatedRows =
                requestRepository
                        .assignMechanic(
                                requestId,
                                mechanic.getId()
                        );


        // =================================================
        // ANOTHER MECHANIC ACCEPTED FIRST
        // =================================================

        if (updatedRows == 0) {

            throw new IllegalStateException(
                    "Service request is no longer available"
            );
        }


        // =================================================
        // RELOAD UPDATED REQUEST
        // =================================================

        ServiceRequest assignedRequest =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Service request not found"
                                )
                        );


        // =================================================
        // STATUS HISTORY
        // =================================================

        addHistory(
                assignedRequest,
                "ASSIGNED",
                userId,
                "Service request accepted by mechanic"
        );


        // =================================================
        // RETURN
        // =================================================

        return toResponse(
                assignedRequest
        );
    }


    // =====================================================
    // HELPER - DRIVER ACTIVE REQUEST
    // =====================================================

    private boolean hasActiveRequest(
            UUID driverId) {


        List<String> activeStatuses =
                List.of(
                        "CREATED",
                        "SEARCHING",
                        "ASSIGNED",
                        "MECHANIC_EN_ROUTE",
                        "ARRIVED",
                        "IN_PROGRESS",
                        "PAYMENT_PENDING"
                );


        return requestRepository
                .findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(
                        driverId,
                        activeStatuses
                )
                .isPresent();
    }


    // =====================================================
    // HELPER - CANCELLABLE
    // =====================================================

    private boolean isCancellable(
            String status) {


        return status.equals("CREATED")
                || status.equals("SEARCHING")
                || status.equals("ASSIGNED");
    }


    // =====================================================
    // HELPER - VALIDATE CATEGORY
    // =====================================================

    private void validateCategory(
            String category) {


        List<String> allowed =
                List.of(
                        "BREAKDOWN",
                        "TYRE",
                        "BATTERY",
                        "FUEL",
                        "OTHER"
                );


        if (!allowed.contains(
                category
        )) {

            throw new IllegalArgumentException(
                    "Invalid service category: "
                            + category
            );
        }
    }


    // =====================================================
    // HELPER - VERIFY DRIVER OWNERSHIP
    // =====================================================

    private void verifyOwnership(
            ServiceRequest request,
            Driver driver) {


        if (!request.getDriver()
                .getId()
                .equals(driver.getId())) {

            throw new IllegalArgumentException(
                    "Service request does not belong to current driver"
            );
        }
    }


    // =====================================================
    // HELPER - ADD HISTORY
    // =====================================================

    private void addHistory(
            ServiceRequest request,
            String status,
            UUID userId,
            String notes) {


        RequestStatusHistory history =
                new RequestStatusHistory();


        history.setRequest(
                request
        );


        history.setStatus(
                status
        );


        history.setChangedByUserId(
                userId
        );


        history.setNotes(
                notes
        );


        historyRepository.save(
                history
        );
    }


    // =====================================================
    // HELPER - SERVICE REQUEST RESPONSE
    // =====================================================

    private ServiceRequestResponse toResponse(
            ServiceRequest request) {


        return new ServiceRequestResponse(

                request.getId(),

                request.getDriver()
                        .getId(),

                request.getVehicle()
                        .getId(),

                request.getCategory(),

                request.getDescription(),

                request.getLatitude(),

                request.getLongitude(),

                request.getAddress(),

                request.getStatus(),

                request.getAssignedMechanicId(),

                request.getCreatedAt(),

                request.getUpdatedAt(),

                request.getCompletedAt(),

                request.getCancelledAt()
        );
    }


    // =====================================================
    // DISTANCE CALCULATION
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
                Math.sin(
                        dLat / 2
                )
                        *
                Math.sin(
                        dLat / 2
                )

                +

                Math.cos(
                        Math.toRadians(
                                lat1
                        )
                )

                *

                Math.cos(
                        Math.toRadians(
                                lat2
                        )
                )

                *

                Math.sin(
                        dLon / 2
                )

                *

                Math.sin(
                        dLon / 2
                );


        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );


        return earthRadiusKm * c;
    }


    // =====================================================
    // INTERNAL REQUEST + DISTANCE
    // =====================================================

    private record RequestWithDistance(
            ServiceRequest request,
            double distanceKm
    ) {
    }
}