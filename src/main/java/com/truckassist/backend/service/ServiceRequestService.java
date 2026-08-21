package com.truckassist.backend.service;

import com.truckassist.backend.dto.CreateServiceRequestRequest;
import com.truckassist.backend.dto.MechanicDriverResponse;
import com.truckassist.backend.dto.MechanicServiceRequestResponse;
import com.truckassist.backend.dto.MechanicVehicleResponse;
import com.truckassist.backend.dto.RequestStatusHistoryResponse;
import com.truckassist.backend.dto.ServiceRequestResponse;
import com.truckassist.backend.dto.VehicleSummaryResponse;
import com.truckassist.backend.dto.MechanicSummaryResponse;

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


// =========================================================
// SERVICE
// =========================================================

@Service
@Transactional
public class ServiceRequestService {


    // =====================================================
    // REPOSITORIES
    // =====================================================

    private final ServiceRequestRepository requestRepository;

    private final RequestStatusHistoryRepository historyRepository;

    private final DriverRepository driverRepository;

    private final VehicleRepository vehicleRepository;

    private final MechanicRepository mechanicRepository;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

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


                    return buildMechanicRequestResponse(
                            request,
                            item.distanceKm()
                    );
                })

                .toList();
    }

// =====================================================
// GET SINGLE REQUEST FOR MECHANIC
// =====================================================

@Transactional(readOnly = true)
public MechanicServiceRequestResponse
getRequestByIdForMechanic(
        UUID userId,
        UUID requestId) {

    System.out.println(
            "=============================================="
    );

    System.out.println(
            "[MECHANIC REQUEST DETAILS] Loading request"
    );

    System.out.println(
            "[MECHANIC REQUEST DETAILS] Mechanic User ID: "
                    + userId
    );

    System.out.println(
            "[MECHANIC REQUEST DETAILS] Request ID: "
                    + requestId
    );


    // =================================================
    // GET CURRENT MECHANIC
    // =================================================

    Mechanic mechanic =
            mechanicRepository
                    .findByUserId(userId)
                    .orElseThrow(() -> {

                        System.out.println(
                                "[MECHANIC REQUEST DETAILS] "
                                        + "Mechanic profile not found"
                        );

                        return new ResourceNotFoundException(
                                "Mechanic profile not found"
                        );
                    });


    System.out.println(
            "[MECHANIC REQUEST DETAILS] Mechanic ID: "
                    + mechanic.getId()
    );

    System.out.println(
            "[MECHANIC REQUEST DETAILS] Available: "
                    + mechanic.isAvailable()
    );


    // =================================================
    // MECHANIC MUST BE ONLINE
    // =================================================

    if (!mechanic.isAvailable()) {

        System.out.println(
                "[MECHANIC REQUEST DETAILS] "
                        + "Mechanic is offline"
        );

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

        System.out.println(
                "[MECHANIC REQUEST DETAILS] "
                        + "Mechanic location missing"
        );

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
                    .orElseThrow(() -> {

                        System.out.println(
                                "[MECHANIC REQUEST DETAILS] "
                                        + "Request not found"
                        );

                        return new ResourceNotFoundException(
                                "Service request not found"
                        );
                    });


    System.out.println(
            "[MECHANIC REQUEST DETAILS] Request status: "
                    + request.getStatus()
    );

    System.out.println(
            "[MECHANIC REQUEST DETAILS] Assigned mechanic ID: "
                    + request.getAssignedMechanicId()
    );


    // =================================================
    // REQUEST ACCESS
    // =================================================
    //
    // SEARCHING
    // ----------
    // Any available mechanic can view.
    //
    // ASSIGNED
    // --------
    // Only the assigned mechanic can view.
    //
    // Other statuses
    // ---------------
    // Not available through this endpoint.
    //
    // =================================================

    String status =
            request.getStatus();


    boolean isSearching =
            "SEARCHING".equalsIgnoreCase(
                    status
            );


    List<String> mechanicAssignedStatuses =
            List.of(
                    "ASSIGNED",
                    "MECHANIC_EN_ROUTE",
                    "ARRIVED",
                    "IN_PROGRESS",
                    "PAYMENT_PENDING"
            );


    boolean isAssignedToCurrentMechanic =
            mechanicAssignedStatuses.contains(
                    status.toUpperCase()
            )
            &&
            request.getAssignedMechanicId() != null
            &&
            request.getAssignedMechanicId()
                    .equals(
                            mechanic.getId()
                    );


    System.out.println(
            "[MECHANIC REQUEST DETAILS] isSearching: "
                    + isSearching
    );

    System.out.println(
            "[MECHANIC REQUEST DETAILS] "
                    + "isAssignedToCurrentMechanic: "
                    + isAssignedToCurrentMechanic
    );


    // =================================================
    // SEARCHING
    // =================================================

    if (isSearching) {

        System.out.println(
                "[MECHANIC REQUEST DETAILS] "
                        + "SEARCHING request - access allowed"
        );
    }


    // =================================================
    // ASSIGNED TO CURRENT MECHANIC
    // =================================================

    else if (isAssignedToCurrentMechanic) {

        System.out.println(
                "[MECHANIC REQUEST DETAILS] "
                        + "ASSIGNED request belongs to current "
                        + "mechanic - access allowed"
        );
    }


    // =================================================
    // NOT AVAILABLE
    // =================================================

    else {

        System.out.println(
                "[MECHANIC REQUEST DETAILS] ACCESS DENIED"
        );

        System.out.println(
                "[MECHANIC REQUEST DETAILS] Status: "
                        + status
        );

        System.out.println(
                "[MECHANIC REQUEST DETAILS] "
                        + "Assigned mechanic: "
                        + request.getAssignedMechanicId()
        );

        System.out.println(
                "[MECHANIC REQUEST DETAILS] "
                        + "Current mechanic: "
                        + mechanic.getId()
        );

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

        System.out.println(
                "[MECHANIC REQUEST DETAILS] "
                        + "Request location missing"
        );

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


    System.out.println(
            "[MECHANIC REQUEST DETAILS] Distance: "
                    + distanceKm
                    + " km"
    );


    // =================================================
    // SERVICE RADIUS
    // =================================================

    if (distanceKm > 10.0) {

        System.out.println(
                "[MECHANIC REQUEST DETAILS] "
                        + "Request outside 10 KM radius"
        );

        throw new IllegalStateException(
                "Service request is outside the service radius"
        );
    }


    // =================================================
    // BUILD RESPONSE
    // =================================================

    MechanicServiceRequestResponse response =
            buildMechanicRequestResponse(
                    request,
                    distanceKm
            );


    System.out.println(
            "[MECHANIC REQUEST DETAILS] Response:"
    );

    System.out.println(
            response
    );

    System.out.println(
            "=============================================="
    );


    return response;
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
    // MECHANIC UPDATE REQUEST STATUS
    // =====================================================
    //
    // Supported flow:
    //
    // ASSIGNED
    //     ↓
    // MECHANIC_EN_ROUTE
    //     ↓
    // ARRIVED
    //     ↓
    // IN_PROGRESS
    //     ↓
    // PAYMENT_PENDING
    //
    // =====================================================

    public ServiceRequestResponse updateMechanicStatus(
            UUID userId,
            UUID requestId,
            String newStatus) {

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
        // MECHANIC MUST BE AVAILABLE
        // =================================================

        if (!mechanic.isAvailable()) {
            throw new IllegalStateException(
                    "Mechanic is not available"
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
        // REQUEST MUST BELONG TO CURRENT MECHANIC
        // =================================================

        if (
                request.getAssignedMechanicId() == null ||
                !request.getAssignedMechanicId()
                        .equals(mechanic.getId())
        ) {
            throw new IllegalStateException(
                    "Service request is not assigned to this mechanic"
            );
        }

        // =================================================
        // NORMALIZE STATUS
        // =================================================

        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Status is required"
            );
        }

        String status =
                newStatus
                        .trim()
                        .toUpperCase();

        String currentStatus =
                request.getStatus();

        // =================================================
        // VALIDATE STATUS TRANSITION
        // =================================================

        boolean validTransition =
                ("ASSIGNED".equals(currentStatus)
                        && "MECHANIC_EN_ROUTE".equals(status))
                ||
                ("MECHANIC_EN_ROUTE".equals(currentStatus)
                        && "ARRIVED".equals(status))
                ||
                ("ARRIVED".equals(currentStatus)
                        && "IN_PROGRESS".equals(status))
                ||
                ("IN_PROGRESS".equals(currentStatus)
                        && "PAYMENT_PENDING".equals(status));

        if (!validTransition) {
            throw new IllegalStateException(
                    "Invalid status transition: "
                            + currentStatus
                            + " -> "
                            + status
            );
        }

        // =================================================
        // UPDATE STATUS
        // =================================================

        request.setStatus(status);

        ServiceRequest saved =
                requestRepository.save(request);

        // =================================================
        // STATUS HISTORY
        // =================================================

        addHistory(
                saved,
                status,
                userId,
                "Status updated by mechanic"
        );

        // =================================================
        // RETURN
        // =================================================

        return toResponse(saved);
    }


    // =====================================================
    // BUILD MECHANIC REQUEST RESPONSE
    // =====================================================
    //
    // This is now used by BOTH:
    //
    // GET /api/v1/mechanics/requests
    //
    // GET /api/v1/mechanics/requests/{requestId}
    //
    // =====================================================

    private MechanicServiceRequestResponse
    buildMechanicRequestResponse(
            ServiceRequest request,
            double distanceKm) {


        if (request == null) {

            throw new IllegalArgumentException(
                    "Service request cannot be null"
            );
        }


        Driver driver =
                request.getDriver();


        Vehicle vehicle =
                request.getVehicle();


        if (driver == null) {

            throw new ResourceNotFoundException(
                    "Driver information not found for service request"
            );
        }


        if (vehicle == null) {

            throw new ResourceNotFoundException(
                    "Vehicle information not found for service request"
            );
        }


        return new MechanicServiceRequestResponse(

                // -----------------------------------------
                // REQUEST
                // -----------------------------------------

                request.getId(),

                // -----------------------------------------
                // DRIVER ID
                // -----------------------------------------

                driver.getId(),

                // -----------------------------------------
                // VEHICLE ID
                // -----------------------------------------

                vehicle.getId(),

                // -----------------------------------------
                // CATEGORY
                // -----------------------------------------

                request.getCategory(),

                // -----------------------------------------
                // DESCRIPTION
                // -----------------------------------------

                request.getDescription(),

                // -----------------------------------------
                // LOCATION
                // -----------------------------------------

                request.getLatitude(),

                request.getLongitude(),

                request.getAddress(),

                // -----------------------------------------
                // STATUS
                // -----------------------------------------

                request.getStatus(),

                // -----------------------------------------
                // CREATED
                // -----------------------------------------

                request.getCreatedAt(),

                // -----------------------------------------
                // DISTANCE
                // -----------------------------------------

                Math.round(
                        distanceKm * 100.0
                ) / 100.0,

                // -----------------------------------------
                // DRIVER DETAILS
                // -----------------------------------------

                buildDriverResponse(
                        driver
                ),

                // -----------------------------------------
                // VEHICLE DETAILS
                // -----------------------------------------

                buildVehicleResponse(
                        vehicle
                )
        );
    }


    // =====================================================
    // BUILD DRIVER RESPONSE
    // =====================================================

    private MechanicDriverResponse
    buildDriverResponse(
            Driver driver) {


        if (driver == null) {

            return null;
        }


        /*
         * Driver -> User
         *
         * The driver's personal information is stored
         * against the User entity.
         */

        if (driver.getUser() == null) {

            return new MechanicDriverResponse(

                    driver.getId(),

                    null,

                    null,

                    null,

                    null
            );
        }


        return new MechanicDriverResponse(

                driver.getId(),

                driver.getUser()
                        .getName(),

                driver.getUser()
                        .getPhone(),

                driver.getUser()
                        .getEmail(),

                driver.getUser()
                        .getProfileImageUrl()
        );
    }


    // =====================================================
    // BUILD VEHICLE RESPONSE
    // =====================================================

    private MechanicVehicleResponse
    buildVehicleResponse(
            Vehicle vehicle) {


        if (vehicle == null) {

            return null;
        }


        return new MechanicVehicleResponse(

                vehicle.getId(),

                vehicle.getRegistrationNumber(),

                vehicle.getManufacturer(),

                vehicle.getModel(),

                vehicle.getVehicleType(),

                vehicle.getManufacturingYear(),

                vehicle.getColor()
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

    VehicleSummaryResponse vehicle =
            new VehicleSummaryResponse(

                    request.getVehicle().getId(),

                    request.getVehicle()
                            .getRegistrationNumber(),

                    request.getVehicle()
                            .getManufacturer(),

                    request.getVehicle()
                            .getModel(),

                    request.getVehicle()
                            .getVehicleType(),

                    request.getVehicle()
                            .getManufacturingYear(),

                    request.getVehicle()
                            .getColor()
            );


    MechanicSummaryResponse mechanic =
            null;


    if (
            request.getAssignedMechanicId()
                    != null
    ) {

        mechanic =
                mechanicRepository
                        .findById(
                                request.getAssignedMechanicId()
                        )
                        .map(m -> {

                            return new MechanicSummaryResponse(

                                    m.getId(),

                                    m.getUser()
                                            .getName(),

                                    m.getUser()
                                            .getPhone(),

                                    m.getUser()
                                            .getProfileImageUrl(),

                                    m.getExperienceYears(),

                                    m.getWorkshopName(),

                                    m.getWorkshopAddress(),

                                    m.getRating(),

                                    m.getTotalJobs(),

                                    m.getLatitude(),

                                    m.getLongitude(),

                                    m.getLastLocationAt()
                            );

                        })
                        .orElse(null);
    }


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

            request.getCancelledAt(),

            vehicle,

            mechanic
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