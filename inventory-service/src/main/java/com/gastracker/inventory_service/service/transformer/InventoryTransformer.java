package com.gastracker.inventory_service.service.transformer;

import com.gastracker.inventory_service.dao.entity.Inventory;
import com.gastracker.inventory_service.dto.response.InventoryResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryTransformer {

    public InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .dealerId(inventory.getDealerId())
                .dealerName(inventory.getDealerName())
                .address(inventory.getAddress())
                .latitude(inventory.getLatitude())
                .longitude(inventory.getLongitude())
                .availableStock(inventory.getAvailableStock())
                .lastUpdated(inventory.getLastUpdated())
                .build();
    }

    public InventoryResponse toResponseWithDistance(Inventory inventory, double distanceKm) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .dealerId(inventory.getDealerId())
                .dealerName(inventory.getDealerName())
                .address(inventory.getAddress())
                .latitude(inventory.getLatitude())
                .longitude(inventory.getLongitude())
                .availableStock(inventory.getAvailableStock())
                .lastUpdated(inventory.getLastUpdated())
                .distanceKm(distanceKm)
                .build();
    }
}
