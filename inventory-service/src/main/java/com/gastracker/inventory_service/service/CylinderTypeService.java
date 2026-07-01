package com.gastracker.inventory_service.service;

import com.gastracker.inventory_service.dao.entity.CylinderType;
import com.gastracker.inventory_service.dao.repository.CylinderTypeRepository;
import com.gastracker.inventory_service.dto.request.CreateCylinderTypeRequest;
import com.gastracker.inventory_service.dto.request.UpdateCylinderTypePriceRequest;
import com.gastracker.inventory_service.dto.response.CylinderTypeResponse;
import com.gastracker.inventory_service.exception.DuplicateResourceException;
import com.gastracker.inventory_service.exception.ResourceNotFoundException;
import com.gastracker.inventory_service.service.transformer.InventoryTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CylinderTypeService {

    private final CylinderTypeRepository cylinderTypeRepository;
    private final InventoryTransformer inventoryTransformer;

    @Transactional
    public CylinderTypeResponse create(CreateCylinderTypeRequest request) {
        if (cylinderTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Cylinder type already exists: " + request.getName());
        }

        CylinderType cylinderType = CylinderType.builder()
                .name(request.getName())
                .capacityKg(request.getCapacityKg())
                .price(request.getPrice())
                .build();

        return inventoryTransformer.toResponse(cylinderTypeRepository.save(cylinderType));
    }

    @Transactional(readOnly = true)
    public List<CylinderTypeResponse> getAll() {
        return cylinderTypeRepository.findAll().stream()
                .map(inventoryTransformer::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CylinderTypeResponse getById(String id) {
        CylinderType ct = cylinderTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cylinder type not found: " + id));
        return inventoryTransformer.toResponse(ct);
    }

    @Transactional
    public CylinderTypeResponse updatePrice(String id, UpdateCylinderTypePriceRequest request) {
        CylinderType ct = cylinderTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cylinder type not found: " + id));
        ct.setPrice(request.getPrice());
        return inventoryTransformer.toResponse(cylinderTypeRepository.save(ct));
    }
}
