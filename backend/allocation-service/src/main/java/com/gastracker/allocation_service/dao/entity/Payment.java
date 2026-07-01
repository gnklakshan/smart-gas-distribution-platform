package com.gastracker.allocation_service.dao.entity;

import com.gastracker.allocation_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String allocationId;

    @Column(nullable = false)
    private String dealerId;

    @Column(nullable = false)
    private BigDecimal amount;   // approvedQuantity * cylinderType.price, in LKR

    @Column(nullable = false)
    private String currency;    // "lkr"

    @Column(nullable = false, unique = true)
    private String stripePaymentIntentId;

    @Column(nullable = false)
    private String clientSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
