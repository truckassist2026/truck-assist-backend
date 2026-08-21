package com.truckassist.backend.service;

import com.truckassist.backend.dto.PaymentInitiateRequest;
import com.truckassist.backend.dto.PaymentMethodRequest;
import com.truckassist.backend.dto.PaymentResponse;
import com.truckassist.backend.entity.Driver;
import com.truckassist.backend.entity.Mechanic;
import com.truckassist.backend.entity.Payment;
import com.truckassist.backend.entity.RequestStatusHistory;
import com.truckassist.backend.entity.ServiceRequest;
import com.truckassist.backend.exception.ResourceNotFoundException;
import com.truckassist.backend.repository.DriverRepository;
import com.truckassist.backend.repository.MechanicRepository;
import com.truckassist.backend.repository.PaymentRepository;
import com.truckassist.backend.repository.RequestStatusHistoryRepository;
import com.truckassist.backend.repository.ServiceRequestRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final ServiceRequestRepository requestRepository;

    private final DriverRepository driverRepository;

    private final MechanicRepository mechanicRepository;

    private final RequestStatusHistoryRepository historyRepository;


    public PaymentService(
            PaymentRepository paymentRepository,
            ServiceRequestRepository requestRepository,
            DriverRepository driverRepository,
            MechanicRepository mechanicRepository,
            RequestStatusHistoryRepository historyRepository) {

        this.paymentRepository =
                paymentRepository;

        this.requestRepository =
                requestRepository;

        this.driverRepository =
                driverRepository;

        this.mechanicRepository =
                mechanicRepository;

        this.historyRepository =
                historyRepository;
    }


    // =====================================================
    // MECHANIC - CREATE PAYMENT
    // =====================================================

    public PaymentResponse initiatePayment(
            UUID userId,
            UUID requestId,
            PaymentInitiateRequest request) {

        Mechanic mechanic =
                mechanicRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Mechanic profile not found"
                                )
                        );


        ServiceRequest serviceRequest =
                getRequest(requestId);


        // =================================================
        // VERIFY MECHANIC OWNERSHIP
        // =================================================

        if (
                serviceRequest
                        .getAssignedMechanicId() == null
                ||
                !serviceRequest
                        .getAssignedMechanicId()
                        .equals(mechanic.getId())
        ) {

            throw new IllegalStateException(
                    "Service request is not assigned to this mechanic"
            );
        }


        // =================================================
        // SERVICE MUST BE IN PROGRESS
        // =================================================

        if (!"IN_PROGRESS".equals(
                serviceRequest.getStatus()
        )) {

            throw new IllegalStateException(
                    "Payment can only be created when service is in progress"
            );
        }


        // =================================================
        // CHECK EXISTING PAYMENT
        // =================================================

        if (
                paymentRepository
                        .findByServiceRequestId(requestId)
                        .isPresent()
        ) {

            throw new IllegalStateException(
                    "Payment already exists for this service request"
            );
        }


        // =================================================
        // VALIDATE AMOUNT
        // =================================================

        if (
                request.amount() == null
                ||
                request.amount()
                        .signum() <= 0
        ) {

            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }


        // =================================================
        // CREATE PAYMENT
        // =================================================

        Payment payment =
                new Payment();

        payment.setServiceRequest(
                serviceRequest
        );

        payment.setAmount(
                request.amount()
        );

        payment.setStatus(
                "PENDING"
        );

        payment.setNotes(
                request.notes()
        );


        Payment savedPayment =
                paymentRepository.save(
                        payment
                );


        // =================================================
        // UPDATE REQUEST
        // =================================================

        serviceRequest.setStatus(
                "PAYMENT_PENDING"
        );

        ServiceRequest savedRequest =
                requestRepository.save(
                        serviceRequest
                );


        // =================================================
        // HISTORY
        // =================================================

        addHistory(
                savedRequest,
                userId,
                "PAYMENT_PENDING",
                "Service completed. Payment of ₹"
                        + request.amount()
                        + " is pending."
        );


        return toResponse(
                savedPayment
        );
    }


    // =====================================================
    // GET PAYMENT
    // =====================================================

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            UUID userId,
            UUID requestId) {

        ServiceRequest request =
                getRequest(requestId);


        authorizeUser(
                userId,
                request
        );


        Payment payment =
                paymentRepository
                        .findByServiceRequestId(
                                requestId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found"
                                )
                        );


        return toResponse(
                payment
        );
    }


    // =====================================================
    // DRIVER - PAY
    // =====================================================

    public PaymentResponse pay(
            UUID userId,
            UUID requestId,
            PaymentMethodRequest request) {

        Driver driver =
                driverRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver profile not found"
                                )
                        );


        ServiceRequest serviceRequest =
                getRequest(requestId);


        // =================================================
        // DRIVER OWNERSHIP
        // =================================================

        if (
                !serviceRequest
                        .getDriver()
                        .getId()
                        .equals(driver.getId())
        ) {

            throw new IllegalArgumentException(
                    "Service request does not belong to current driver"
            );
        }


        // =================================================
        // STATUS
        // =================================================

        if (!"PAYMENT_PENDING".equals(
                serviceRequest.getStatus()
        )) {

            throw new IllegalStateException(
                    "Payment is not pending for this request"
            );
        }


        Payment payment =
                paymentRepository
                        .findByServiceRequestId(
                                requestId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found"
                                )
                        );


        if (!"PENDING".equals(
                payment.getStatus()
        )) {

            throw new IllegalStateException(
                    "Payment has already been processed"
            );
        }


        // =================================================
        // METHOD
        // =================================================

        String method =
                request.paymentMethod()
                        .trim()
                        .toUpperCase();


        List<String> allowedMethods =
                List.of(
                        "CASH",
                        "UPI",
                        "CARD"
                );


        if (!allowedMethods.contains(
                method
        )) {

            throw new IllegalArgumentException(
                    "Invalid payment method. Allowed: CASH, UPI, CARD"
            );
        }


        // =================================================
        // MARK PAID
        // =================================================

        payment.setPaymentMethod(
                method
        );

        payment.setStatus(
                "PAID"
        );

        payment.setPaidAt(
                OffsetDateTime.now()
        );


        Payment savedPayment =
                paymentRepository.save(
                        payment
                );


        // =================================================
        // COMPLETE REQUEST
        // =================================================

        serviceRequest.setStatus(
                "COMPLETED"
        );

        serviceRequest.setCompletedAt(
                OffsetDateTime.now()
        );


        ServiceRequest savedRequest =
                requestRepository.save(
                        serviceRequest
                );


        // =================================================
        // HISTORY
        // =================================================

        addHistory(
                savedRequest,
                userId,
                "COMPLETED",
                "Payment received via "
                        + method
                        + ". Amount ₹"
                        + payment.getAmount()
        );


        return toResponse(
                savedPayment
        );
    }


    // =====================================================
    // GET REQUEST
    // =====================================================

    private ServiceRequest getRequest(
            UUID requestId) {

        return requestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Service request not found"
                        )
                );
    }


    // =====================================================
    // AUTHORIZATION
    // =====================================================

    private void authorizeUser(
            UUID userId,
            ServiceRequest request) {

        boolean driverOwner =
                request.getDriver()
                        .getUser()
                        .getId()
                        .equals(userId);


        boolean assignedMechanic =
                request.getAssignedMechanicId()
                        != null
                &&
                mechanicRepository
                        .findByUserId(userId)
                        .map(mechanic ->
                                mechanic.getId()
                                        .equals(
                                                request
                                                        .getAssignedMechanicId()
                                        )
                        )
                        .orElse(false);


        if (
                !driverOwner
                &&
                !assignedMechanic
        ) {

            throw new IllegalArgumentException(
                    "You are not authorized to access this payment"
            );
        }
    }


    // =====================================================
    // HISTORY
    // =====================================================

    private void addHistory(
            ServiceRequest request,
            UUID userId,
            String status,
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
    // RESPONSE
    // =====================================================

    private PaymentResponse toResponse(
            Payment payment) {

        return new PaymentResponse(

                payment.getId(),

                payment.getServiceRequest()
                        .getId(),

                payment.getAmount(),

                payment.getStatus(),

                payment.getPaymentMethod(),

                payment.getNotes(),

                payment.getCreatedAt(),

                payment.getPaidAt()
        );
    }
}