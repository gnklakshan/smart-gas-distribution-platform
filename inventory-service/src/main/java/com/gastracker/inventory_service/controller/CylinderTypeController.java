package com.gastracker.inventory_service.controller;

import com.gastracker.inventory_service.dto.request.CreateCylinderTypeRequest;
import com.gastracker.inventory_service.dto.request.UpdateCylinderTypePriceRequest;
import com.gastracker.inventory_service.dto.response.CylinderTypeResponse;
import com.gastracker.inventory_service.service.CylinderTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cylinder-types")
@RequiredArgsConstructor
public class CylinderTypeController {

    private final CylinderTypeService cylinderTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CylinderTypeResponse> create(@Valid @RequestBody CreateCylinderTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cylinderTypeService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CylinderTypeResponse>> getAll() {
        return ResponseEntity.ok(cylinderTypeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CylinderTypeResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(cylinderTypeService.getById(id));
    }

    @PutMapping("/{id}/price")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CylinderTypeResponse> updatePrice(
            @PathVariable String id,
            @Valid @RequestBody UpdateCylinderTypePriceRequest request) {
        return ResponseEntity.ok(cylinderTypeService.updatePrice(id, request));
    }
}
