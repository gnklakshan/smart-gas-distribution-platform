package com.gastracker.inventory_service.dao.entity;

import com.gastracker.inventory_service.enums.StockChangeReason;
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
@Table(name = "stock_history")
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "inventory_id", nullable = false)
    private String inventoryId;

    @Column(name = "dealer_id", nullable = false)
    private String dealerId;

    @Column(name = "cylinder_type_id", nullable = false)
    private String cylinderTypeId;

    @Column(name = "previous_stock", nullable = false)
    private Integer previousStock;

    @Column(name = "new_stock", nullable = false)
    private Integer newStock;

    @Column(nullable = false)
    private Integer change;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockChangeReason reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
