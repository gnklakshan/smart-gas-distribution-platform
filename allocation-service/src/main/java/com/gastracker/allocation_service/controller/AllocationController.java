package com.gastracker.allocation_service.controller;

import com.gastracker.allocation_service.dto.request.AllocationRequest;
import com.gastracker.allocation_service.dto.request.ApproveRequest;
import com.gastracker.allocation_service.dto.request.RejectRequest;
import com.gastracker.allocation_service.dto.response.AllocationAnalyticsResponse;
import com.gastracker.allocation_service.dto.response.AllocationResponse;
import com.gastracker.allocation_service.enums.AllocationStatus;
import com.gastracker.allocation_service.service.AllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/allocations")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocationService allocationService;

    // ── Health check ───────────────────────────────────────────────────────
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "allocation-service",
                "message", "Allocation Service is running"
        ));
    }

    // ── DEALER: submit a new allocation request ────────────────────────────
    @PostMapping("/request")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<AllocationResponse> requestAllocation(
            @Valid @RequestBody AllocationRequest request,
            Authentication authentication) {

        String dealerId = (String) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(allocationService.requestAllocation(dealerId, request));
    }

    // ── ADMIN: list all pending allocations ───────────────────────────────
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AllocationResponse>> getPending() {
        return ResponseEntity.ok(allocationService.getPending());
    }

    // ── ADMIN: list all allocations (optional status filter) ──────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AllocationResponse>> getAll(
            @RequestParam(required = false) AllocationStatus status) {
        return ResponseEntity.ok(allocationService.getAll(status));
    }

    // ── ADMIN: approve a pending allocation ───────────────────────────────
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AllocationResponse> approve(
            @PathVariable String id,
            @Valid @RequestBody ApproveRequest request) {
        return ResponseEntity.ok(allocationService.approveAllocation(id, request));
    }

    // ── ADMIN: reject a pending allocation ────────────────────────────────
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AllocationResponse> reject(
            @PathVariable String id,
            @Valid @RequestBody RejectRequest request) {
        return ResponseEntity.ok(allocationService.rejectAllocation(id, request));
    }

    // ── DEALER: confirm physical delivery (triggers Kafka event) ──────────
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<AllocationResponse> confirm(
            @PathVariable String id,
            Authentication authentication) {

        String dealerId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(allocationService.confirmDelivery(id, dealerId));
    }

    // ── DEALER: view own allocation history ───────────────────────────────
    @GetMapping("/dealer/{dealerId}")
    @PreAuthorize("hasRole('DEALER') and #dealerId == authentication.principal")
    public ResponseEntity<List<AllocationResponse>> getByDealer(@PathVariable String dealerId) {
        return ResponseEntity.ok(allocationService.getByDealer(dealerId));
    }

    // ── DEALER: allocation analytics + fair-distribution comparison ───────
    @GetMapping("/dealer/{dealerId}/analytics")
    @PreAuthorize("hasRole('DEALER') and #dealerId == authentication.principal")
    public ResponseEntity<AllocationAnalyticsResponse> getAnalytics(@PathVariable String dealerId) {
        return ResponseEntity.ok(allocationService.getAnalytics(dealerId));
    }

    // ── Any authenticated user: get single allocation by ID ───────────────
    @GetMapping("/{id}")
    public ResponseEntity<AllocationResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(allocationService.getById(id));
    }
}
