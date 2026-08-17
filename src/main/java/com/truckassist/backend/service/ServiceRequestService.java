package com.truckassist.backend.service;

import com.truckassist.backend.dto.CreateServiceRequestRequest;
import com.truckassist.backend.dto.RequestStatusHistoryResponse;
import com.truckassist.backend.dto.ServiceRequestResponse;
import com.truckassist.backend.entity.Driver;
import com.truckassist.backend.entity.RequestStatusHistory;
import com.truckassist.backend.entity.ServiceRequest;
import com.truckassist.backend.entity.Vehicle;
import com.truckassist.backend.exception.ResourceNotFoundException;
import com.truckassist.backend.repository.DriverRepository;
import com.truckassist.backend.repository.RequestStatusHistoryRepository;
import com.truckassist.backend.repository.ServiceRequestRepository;
import com.truckassist.backend.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final RequestStatusHistoryRepository historyRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public ServiceRequestService(
            ServiceRequestRepository requestRepository,
            RequestStatusHistoryRepository historyRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository) {

        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // =====================================================
    // CREATE REQUEST
    // =====================================================

    public ServiceRequestResponse create(
            UUID userId,
            CreateServiceRequestRequest request) {

        Driver driver =
                driverRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver profile not found"
                                )
                        );

        Vehicle vehicle =
                vehicleRepository
                        .findById(request.vehicleId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vehicle not found"
                                )
                        );

        // Vehicle must belong to current driver
        if (!vehicle.getDriver()
                .getId()
                .equals(driver.getId())) {

            throw new IllegalArgumentException(
                    "Vehicle does not belong to current driver"
            );
        }

        // Driver should not create another active request
        if (hasActiveRequest(driver.getId())) {

            throw new IllegalStateException(
                    "You already have an active service request"
            );
        }

        String category =
                request.category()
                        .trim()
                        .toUpperCase();

        validateCategory(category);

        ServiceRequest serviceRequest =
                new ServiceRequest();

        serviceRequest.setDriver(driver);
        serviceRequest.setVehicle(vehicle);
        serviceRequest.setCategory(category);
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

        return toResponse(saved);
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

        return toResponse(request);
    }

    // =====================================================
    // GET REQUEST BY ID
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

        return toResponse(request);
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
                                history.getRequest().getId(),
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

        if (!isCancellable(request.getStatus())) {

            throw new IllegalStateException(
                    "Request cannot be cancelled at status: "
                            + request.getStatus()
            );
        }

        request.setStatus("CANCELLED");
        request.setCancelledAt(
                OffsetDateTime.now()
        );

        ServiceRequest saved =
                requestRepository.save(request);

        addHistory(
                saved,
                "CANCELLED",
                userId,
                "Cancelled by driver"
        );

        return toResponse(saved);
    }

    // =====================================================
    // HELPERS
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

    private boolean isCancellable(
            String status) {

        return status.equals("CREATED")
                || status.equals("SEARCHING")
                || status.equals("ASSIGNED");
    }

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

        if (!allowed.contains(category)) {

            throw new IllegalArgumentException(
                    "Invalid service category: "
                            + category
            );
        }
    }

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

    private void addHistory(
            ServiceRequest request,
            String status,
            UUID userId,
            String notes) {

        RequestStatusHistory history =
                new RequestStatusHistory();

        history.setRequest(request);
        history.setStatus(status);
        history.setChangedByUserId(userId);
        history.setNotes(notes);

        historyRepository.save(history);
    }

    private ServiceRequestResponse toResponse(
            ServiceRequest request) {

        return new ServiceRequestResponse(
                request.getId(),
                request.getDriver().getId(),
                request.getVehicle().getId(),
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
}