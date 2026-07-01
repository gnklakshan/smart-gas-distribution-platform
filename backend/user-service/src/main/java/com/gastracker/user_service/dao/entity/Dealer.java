package com.gastracker.user_service.dao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "dealers")
public class Dealer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false)
    private String businessName;

    @Column(unique = true)
    private String businessRegNo;

    @Column(nullable = false)
    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
