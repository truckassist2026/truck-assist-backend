package com.truckassist.backend.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "request_status_history")
@Getter
@Setter
public class RequestStatusHistory {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    // =====================================================
    // REQUEST
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "request_id",
            nullable = false
    )
    private ServiceRequest request;


    // =====================================================
    // STATUS
    // =====================================================

    @Column(
            nullable = false,
            length = 30
    )
    private String status;


    // =====================================================
    // USER WHO CHANGED STATUS
    // =====================================================

    @Column(
            name = "changed_by_user_id"
    )
    private UUID changedByUserId;


    @Column(
            length = 1000
    )
    private String notes;


    // =====================================================
    // CREATED
    // =====================================================

    @Column(
            name = "created_at",
            nullable = false
    )
    private OffsetDateTime createdAt;


    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {

            createdAt =
                    OffsetDateTime.now();
        }
    }
}