package com.gastracker.inventory_service.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cylinder_types")
public class CylinderType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;    // e.g., '12.5kg Domestic', '2.5kg Camping'

    @Column(nullable = false)
    private BigDecimal capacityKg;
}
