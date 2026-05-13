package com.gastracker.inventory_service.dao.repository;

import com.gastracker.inventory_service.dao.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

    List<Inventory> findByDealerId(String dealerId);

    Optional<Inventory> findByDealerIdAndCylinderTypeId(String dealerId, String cylinderTypeId);

    boolean existsByDealerIdAndCylinderTypeId(String dealerId, String cylinderTypeId);

    List<Inventory> findByAvailableStockGreaterThan(int availableStock);

    List<Inventory> findByCylinderTypeId(String cylinderTypeId);
}
