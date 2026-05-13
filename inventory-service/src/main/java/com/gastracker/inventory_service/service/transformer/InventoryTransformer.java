package com.gastracker.inventory_service.service.transformer;

import com.gastracker.inventory_service.dao.entity.CylinderType;
import com.gastracker.inventory_service.dao.entity.Inventory;
import com.gastracker.inventory_service.dto.response.CylinderTypeResponse;
import com.gastracker.inventory_service.dto.response.InventoryResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryTransformer {

    public InventoryResponse toResponse(Inventory inventory, String cylinderTypeName) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .dealerId(inventory.getDealerId())
                .cylinderTypeId(inventory.getCylinderTypeId())
                .cylinderTypeName(cylinderTypeName)
                .availableStock(inventory.getAvailableStock())
                .lastUpdated(inventory.getLastUpdated())
                .build();
    }

    public CylinderTypeResponse toResponse(CylinderType cylinderType) {
        return CylinderTypeResponse.builder()
                .id(cylinderType.getId())
                .name(cylinderType.getName())
                .capacityKg(cylinderType.getCapacityKg())
                .build();
    }
}
