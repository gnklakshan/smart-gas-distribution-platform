package com.gastracker.inventory_service.service;

import com.gastracker.inventory_service.client.UserClient;
import com.gastracker.inventory_service.dao.entity.CylinderType;
import com.gastracker.inventory_service.dao.entity.Inventory;
import com.gastracker.inventory_service.dao.repository.CylinderTypeRepository;
import com.gastracker.inventory_service.dao.repository.InventoryRepository;
import com.gastracker.inventory_service.dto.request.CreateInventoryRequest;
import com.gastracker.inventory_service.dto.request.UpdateStockRequest;
import com.gastracker.inventory_service.dto.response.InventoryResponse;
import com.gastracker.inventory_service.dto.response.UserProfileResponse;
import com.gastracker.inventory_service.exception.DuplicateResourceException;
import com.gastracker.inventory_service.exception.ForbiddenOperationException;
import com.gastracker.inventory_service.exception.ResourceNotFoundException;
import com.gastracker.inventory_service.service.transformer.InventoryTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final CylinderTypeRepository cylinderTypeRepository;
    private final InventoryTransformer inventoryTransformer;
    private final UserClient userClient;

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (inventoryRepository.existsByDealerIdAndCylinderTypeId(request.getDealerId(), request.getCylinderTypeId())) {
            throw new DuplicateResourceException(
                    "Inventory already exists for dealer: " + request.getDealerId() +
                    " and cylinder type: " + request.getCylinderTypeId());
        }

        CylinderType cylinderType = findCylinderType(request.getCylinderTypeId());

        Inventory inventory = Inventory.builder()
                .dealerId(request.getDealerId())
                .cylinderTypeId(request.getCylinderTypeId())
                .availableStock(request.getAvailableStock())
                .build();

        return inventoryTransformer.toResponse(inventoryRepository.save(inventory), cylinderType.getName());
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(String id) {
        Inventory inventory = findInventory(id);
        CylinderType ct = findCylinderType(inventory.getCylinderTypeId());
        return inventoryTransformer.toResponse(inventory, ct.getName());
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByDealerId(String dealerId) {
        return inventoryRepository.findByDealerId(dealerId).stream()
                .map(inv -> {
                    String ctName = cylinderTypeRepository.findById(inv.getCylinderTypeId())
                            .map(CylinderType::getName).orElse("Unknown");
                    return inventoryTransformer.toResponse(inv, ctName);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAvailableInventory(String authHeader) {
        List<Inventory> available = inventoryRepository.findByAvailableStockGreaterThan(0);

        // fetch dealer info once per unique dealerId
        Map<String, UserProfileResponse> dealerCache = available.stream()
                .map(Inventory::getDealerId)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            try { return userClient.getUserById(authHeader, id); }
                            catch (Exception e) {
                                log.warn("Could not fetch dealer info for dealerId={}: {}", id, e.getMessage());
                                return null;
                            }
                        }
                ));

        return available.stream()
                .map(inv -> {
                    String ctName = cylinderTypeRepository.findById(inv.getCylinderTypeId())
                            .map(CylinderType::getName).orElse("Unknown");
                    InventoryResponse resp = inventoryTransformer.toResponse(inv, ctName);

                    UserProfileResponse dealer = dealerCache.get(inv.getDealerId());
                    if (dealer != null && dealer.getDealer() != null) {
                        UserProfileResponse.DealerDetail d = dealer.getDealer();
                        resp.setDealerName(d.getBusinessName());
                        resp.setAddress(d.getAddress());
                        resp.setLatitude(d.getLatitude() != null ? d.getLatitude().doubleValue() : null);
                        resp.setLongitude(d.getLongitude() != null ? d.getLongitude().doubleValue() : null);
                    }
                    return resp;
                })
                .toList();
    }

    @Transactional
    public InventoryResponse updateStock(String inventoryId, String dealerId, UpdateStockRequest request) {
        Inventory inventory = findInventory(inventoryId);

        if (!inventory.getDealerId().equals(dealerId)) {
            throw new ForbiddenOperationException("You can only update your own inventory");
        }

        inventory.setAvailableStock(request.getAvailableStock());
        CylinderType ct = findCylinderType(inventory.getCylinderTypeId());
        return inventoryTransformer.toResponse(inventoryRepository.save(inventory), ct.getName());
    }

    /**
     * Called by Kafka consumer when allocation is confirmed (add stock).
     */
    @Transactional
    public void addStock(String dealerId, String cylinderTypeId, int quantity) {
        Inventory inventory = inventoryRepository.findByDealerIdAndCylinderTypeId(dealerId, cylinderTypeId)
                .orElseGet(() -> {
                    log.info("Creating new inventory record for dealer={} cylinderType={}", dealerId, cylinderTypeId);
                    return inventoryRepository.save(Inventory.builder()
                            .dealerId(dealerId)
                            .cylinderTypeId(cylinderTypeId)
                            .availableStock(0)
                            .build());
                });

        inventory.setAvailableStock(inventory.getAvailableStock() + quantity);
        inventoryRepository.save(inventory);
        log.info("Added {} stock for dealer={} cylinderType={}, new total={}", quantity, dealerId, cylinderTypeId, inventory.getAvailableStock());
    }

    /**
     * Called by Kafka consumer when queue pickup is completed (subtract stock).
     */
    @Transactional
    public void subtractStock(String dealerId, String cylinderTypeId, int quantity) {
        Inventory inventory = inventoryRepository.findByDealerIdAndCylinderTypeId(dealerId, cylinderTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for dealer: " + dealerId + " cylinderType: " + cylinderTypeId));

        int newStock = Math.max(0, inventory.getAvailableStock() - quantity);
        inventory.setAvailableStock(newStock);
        inventoryRepository.save(inventory);
        log.info("Subtracted {} stock for dealer={} cylinderType={}, new total={}", quantity, dealerId, cylinderTypeId, newStock);
    }

    private Inventory findInventory(String id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));
    }

    private CylinderType findCylinderType(String id) {
        return cylinderTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cylinder type not found: " + id));
    }
}
