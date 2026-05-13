package com.gastracker.queue_service.controller;

import com.gastracker.queue_service.dto.request.JoinQueueRequest;
import com.gastracker.queue_service.dto.response.QueueResponse;
import com.gastracker.queue_service.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    // ── CITIZEN: join a queue at a dealer ──────────────────────────────────
    @PostMapping("/join")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<QueueResponse> joinQueue(
            @Valid @RequestBody JoinQueueRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(queueService.joinQueue(userId, request));
    }

    // ── CITIZEN: get my queue entries ──────────────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<QueueResponse>> getMyQueues(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(queueService.getMyQueues(userId));
    }

    // ── DEALER: get queue for my shop ──────────────────────────────────────
    @GetMapping("/dealer/{dealerId}")
    @PreAuthorize("hasRole('DEALER') and #dealerId == authentication.principal")
    public ResponseEntity<List<QueueResponse>> getDealerQueue(@PathVariable String dealerId) {
        return ResponseEntity.ok(queueService.getDealerQueue(dealerId));
    }

    // ── DEALER: mark citizen as ready for pickup ──────────────────────────
    @PutMapping("/{id}/ready")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<QueueResponse> markReady(
            @PathVariable String id,
            Authentication authentication) {
        String dealerId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(queueService.markReady(id, dealerId));
    }

    // ── DEALER: mark pickup as completed ──────────────────────────────────
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<QueueResponse> markCompleted(
            @PathVariable String id,
            Authentication authentication) {
        String dealerId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(queueService.markCompleted(id, dealerId));
    }

    // ── CITIZEN or DEALER: cancel a queue entry ───────────────────────────
    @PutMapping("/{id}/cancel")
    public ResponseEntity<QueueResponse> cancelQueue(
            @PathVariable String id,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(queueService.cancelQueue(id, userId));
    }

    // ── Any authenticated user: get single queue entry ────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<QueueResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(queueService.getById(id));
    }
}
