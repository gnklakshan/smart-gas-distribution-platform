package com.gastracker.inventory_service.controller;

import com.gastracker.inventory_service.dto.request.CreateInventoryRequest;
import com.gastracker.inventory_service.dto.request.UpdateStockRequest;
import com.gastracker.inventory_service.dto.response.InventoryResponse;
import com.gastracker.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createInventory(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponse> getInventoryById(@PathVariable String id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @GetMapping("/dealer/{dealerId}")
    public ResponseEntity<InventoryResponse> getInventoryByDealerId(@PathVariable String dealerId) {
        return ResponseEntity.ok(inventoryService.getInventoryByDealerId(dealerId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<InventoryResponse>> getAvailableInventory() {
        return ResponseEntity.ok(inventoryService.getAvailableInventory());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<InventoryResponse>> getNearbyInventory(
            @RequestParam("lat") double latitude,
            @RequestParam("lng") double longitude,
            @RequestParam(value = "radius", defaultValue = "10") double radiusKm) {
        return ResponseEntity.ok(inventoryService.getNearbyInventory(latitude, longitude, radiusKm));
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<InventoryResponse> updateStock(
            @PathVariable String id,
            @Valid @RequestBody UpdateStockRequest request,
            Authentication authentication) {
        String dealerId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(inventoryService.updateStock(id, dealerId, request));
    }

    @GetMapping("/location/{location}")
    public ResponseEntity<List<InventoryResponse>> getInventoryByLocation(@PathVariable String location) {
        return ResponseEntity.ok(inventoryService.getInventoryByLocation(location));
    }
}
