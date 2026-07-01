package com.gastracker.allocation_service.controller;

import com.gastracker.allocation_service.dto.response.PaymentResponse;
import com.gastracker.allocation_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ── DEALER: create a Stripe PaymentIntent for an approved allocation ──
    @PostMapping("/allocations/{allocationId}")
    @PreAuthorize("hasRole('DEALER')")
    public ResponseEntity<PaymentResponse> createPaymentIntent(
            @PathVariable String allocationId,
            Authentication authentication,
            @RequestHeader("Authorization") String authHeader) {

        String dealerId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(paymentService.createPaymentIntent(allocationId, dealerId, authHeader));
    }

    // ── DEALER/ADMIN: check payment status for an allocation ──
    @GetMapping("/allocations/{allocationId}")
    public ResponseEntity<PaymentResponse> getByAllocation(@PathVariable String allocationId) {
        return ResponseEntity.ok(paymentService.getByAllocationId(allocationId));
    }

    // ── Stripe webhook — no auth, verified via signature instead ──
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
