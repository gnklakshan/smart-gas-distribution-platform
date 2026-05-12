package com.gastracker.inventory_service.dao.repository;

import com.gastracker.inventory_service.dao.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

    Optional<Inventory> findByDealerId(String dealerId);

    boolean existsByDealerId(String dealerId);

    List<Inventory> findByAvailableStockGreaterThan(int availableStock);

    List<Inventory> findByAddressContainingIgnoreCase(String location);
}
