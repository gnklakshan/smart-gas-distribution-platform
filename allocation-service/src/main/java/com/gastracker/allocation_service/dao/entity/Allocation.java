package com.gastracker.allocation_service.dao.entity;

import com.gastracker.allocation_service.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "allocations")
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String dealerId;            // UUID from user-service (JWT principal)

    @Column(nullable = false)
    private String dealerName;

    @Column(nullable = false)
    private Integer requestedQuantity;

    private Integer approvedQuantity;   // set by admin on approval

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status;

    private String rejectionReason;     // set by admin on rejection

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime resolvedAt;   // when approved or rejected

    private LocalDateTime deliveredAt;  // when dealer confirms

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }
}
