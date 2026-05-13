package com.gastracker.inventory_service.dao.repository;

import com.gastracker.inventory_service.dao.entity.CylinderType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CylinderTypeRepository extends JpaRepository<CylinderType, String> {
    boolean existsByName(String name);
}
