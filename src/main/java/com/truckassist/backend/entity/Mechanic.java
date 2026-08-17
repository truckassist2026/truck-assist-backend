package com.truckassist.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mechanics")
@Getter
@Setter
public class Mechanic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // =====================================================
    // USER
    // =====================================================

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    // =====================================================
    // PROFILE
    // =====================================================

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "workshop_name")
    private String workshopName;

    @Column(name = "workshop_address")
    private String workshopAddress;

    // =====================================================
    // AVAILABILITY
    // =====================================================

    @Column(name = "is_available", nullable = false)
    private boolean available;

    // =====================================================
    // RATING
    // =====================================================

    @Column(nullable = false)
    private BigDecimal rating;

    @Column(name = "total_jobs", nullable = false)
    private Integer totalJobs;

    // =====================================================
    // LOCATION
    // =====================================================

    @Column(
            precision = 10,
            scale = 7
    )
    private BigDecimal latitude;

    @Column(
            precision = 10,
            scale = 7
    )
    private BigDecimal longitude;

    @Column(name = "last_location_at")
    private OffsetDateTime lastLocationAt;

    // =====================================================
    // TIMESTAMPS
    // =====================================================

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // =====================================================
    // JPA
    // =====================================================

    @PrePersist
    protected void onCreate() {

        OffsetDateTime now =
                OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (rating == null) {
            rating = BigDecimal.ZERO;
        }

        if (totalJobs == null) {
            totalJobs = 0;
        }

        // New mechanic starts offline
        available = false;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                OffsetDateTime.now();
    }
}