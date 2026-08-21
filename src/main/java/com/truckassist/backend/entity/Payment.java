package com.truckassist.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payments_service_request",
                        columnNames = "service_request_id"
                )
        }
)
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // =====================================================
    // SERVICE REQUEST
    // =====================================================

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "service_request_id",
            nullable = false,
            unique = true
    )
    private ServiceRequest serviceRequest;

    // =====================================================
    // AMOUNT
    // =====================================================

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    // =====================================================
    // STATUS
    // =====================================================

    @Column(
            nullable = false,
            length = 30
    )
    private String status;

    // PENDING / PAID

    // =====================================================
    // PAYMENT METHOD
    // =====================================================

    @Column(
            name = "payment_method",
            length = 30
    )
    private String paymentMethod;

    // CASH / UPI / CARD

    // =====================================================
    // NOTES
    // =====================================================

    @Column(length = 500)
    private String notes;

    // =====================================================
    // TIMESTAMPS
    // =====================================================

    @Column(
            name = "created_at",
            nullable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    // =====================================================
    // JPA
    // =====================================================

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt =
                    OffsetDateTime.now();
        }

        if (status == null) {
            status = "PENDING";
        }
    }
}