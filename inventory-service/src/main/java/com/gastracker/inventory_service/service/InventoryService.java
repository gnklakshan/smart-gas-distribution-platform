package com.gastracker.inventory_service.service;

import com.gastracker.inventory_service.dao.entity.Inventory;
import com.gastracker.inventory_service.dao.repository.InventoryRepository;
import com.gastracker.inventory_service.dto.request.CreateInventoryRequest;
import com.gastracker.inventory_service.dto.request.UpdateStockRequest;
import com.gastracker.inventory_service.dto.response.InventoryResponse;
import com.gastracker.inventory_service.exception.DuplicateResourceException;
import com.gastracker.inventory_service.exception.ForbiddenOperationException;
import com.gastracker.inventory_service.exception.ResourceNotFoundException;
import com.gastracker.inventory_service.service.transformer.InventoryTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransformer inventoryTransformer;

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (inventoryRepository.existsByDealerId(request.getDealerId())) {
            throw new DuplicateResourceException("Inventory already exists for dealer: " + request.getDealerId());
        }

        Inventory inventory = Inventory.builder()
                .dealerId(request.getDealerId())
                .dealerName(request.getDealerName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .availableStock(request.getAvailableStock())
                .build();

        return inventoryTransformer.toResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(String id) {
        return inventoryTransformer.toResponse(findInventory(id));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByDealerId(String dealerId) {
        Inventory inventory = inventoryRepository.findByDealerId(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for dealer: " + dealerId));
        return inventoryTransformer.toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAvailableInventory() {
        return inventoryRepository.findByAvailableStockGreaterThan(0).stream()
                .map(inventoryTransformer::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getNearbyInventory(double latitude, double longitude, double radiusKm) {
        validateRadius(radiusKm);

        return inventoryRepository.findByAvailableStockGreaterThan(0).stream()
                .filter(inventory -> inventory.getLatitude() != null && inventory.getLongitude() != null)
                .map(inventory -> {
                    double distance = calculateDistance(
                            latitude,
                            longitude,
                            inventory.getLatitude(),
                            inventory.getLongitude()
                    );
                    return new NearbyInventory(inventory, distance);
                })
                .filter(result -> result.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyInventory::distanceKm))
                .map(result -> inventoryTransformer.toResponseWithDistance(result.inventory(), roundDistance(result.distanceKm())))
                .toList();
    }

    @Transactional
    public InventoryResponse updateStock(String inventoryId, String dealerId, UpdateStockRequest request) {
        Inventory inventory = findInventory(inventoryId);

        if (!inventory.getDealerId().equals(dealerId)) {
            throw new ForbiddenOperationException("You can only update your own inventory");
        }

        inventory.setAvailableStock(request.getAvailableStock());
        return inventoryTransformer.toResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByLocation(String location) {
        return inventoryRepository.findByAddressContainingIgnoreCase(location).stream()
                .map(inventoryTransformer::toResponse)
                .toList();
    }

    private Inventory findInventory(String id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));
    }

    private void validateRadius(double radiusKm) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("radius must be greater than 0");
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double radiusOfEarthKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return radiusOfEarthKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double roundDistance(double distance) {
        return Math.round(distance * 100.0) / 100.0;
    }

    private record NearbyInventory(Inventory inventory, double distanceKm) {
    }
}
