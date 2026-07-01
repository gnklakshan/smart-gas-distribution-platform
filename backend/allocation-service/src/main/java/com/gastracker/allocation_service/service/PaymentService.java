package com.gastracker.allocation_service.service;

import com.gastracker.allocation_service.client.InventoryClient;
import com.gastracker.allocation_service.config.StripeConfig;
import com.gastracker.allocation_service.dao.entity.Allocation;
import com.gastracker.allocation_service.dao.entity.Payment;
import com.gastracker.allocation_service.dao.repository.AllocationRepository;
import com.gastracker.allocation_service.dao.repository.PaymentRepository;
import com.gastracker.allocation_service.dto.response.CylinderTypeResponse;
import com.gastracker.allocation_service.dto.response.PaymentResponse;
import com.gastracker.allocation_service.enums.AllocationStatus;
import com.gastracker.allocation_service.enums.PaymentStatus;
import com.gastracker.allocation_service.event.PaymentConfirmedEvent;
import com.gastracker.allocation_service.exception.InvalidStateException;
import com.gastracker.allocation_service.exception.ResourceNotFoundException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String TOPIC_PAYMENT_CONFIRMED = "payment.confirmed";

    private final PaymentRepository paymentRepository;
    private final AllocationRepository allocationRepository;
    private final InventoryClient inventoryClient;
    private final StripeConfig stripeConfig;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── DEALER: create (or fetch existing) PaymentIntent for an approved allocation ──
    @Transactional
    public PaymentResponse createPaymentIntent(String allocationId, String dealerId, String authHeader) {
        Allocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + allocationId));

        if (!allocation.getDealerId().equals(dealerId)) {
            throw new AccessDeniedException("You do not own this allocation");
        }
        if (allocation.getStatus() != AllocationStatus.APPROVED) {
            throw new InvalidStateException("Payment can only be made for APPROVED allocations. Current status: " + allocation.getStatus());
        }

        Optional<Payment> existing = paymentRepository.findByAllocationId(allocationId);
        if (existing.isPresent() && existing.get().getStatus() != PaymentStatus.FAILED) {
            return toResponse(existing.get());
        }

        CylinderTypeResponse cylinderType = inventoryClient.getCylinderTypeById(authHeader, allocation.getCylinderTypeId());
        BigDecimal amount = cylinderType.getPrice().multiply(BigDecimal.valueOf(allocation.getApprovedQuantity()));

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toSmallestUnit(amount))
                    .setCurrency(stripeConfig.getCurrency())
                    .putMetadata("allocationId", allocationId)
                    .putMetadata("dealerId", dealerId)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Payment payment = existing.orElseGet(() -> Payment.builder().allocationId(allocationId).build());
            payment.setDealerId(dealerId);
            payment.setAmount(amount);
            payment.setCurrency(stripeConfig.getCurrency());
            payment.setStripePaymentIntentId(intent.getId());
            payment.setClientSecret(intent.getClientSecret());
            payment.setStatus(PaymentStatus.PENDING);

            return toResponse(paymentRepository.save(payment));
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed for allocation {}: {}", allocationId, e.getMessage());
            throw new InvalidStateException("Could not initiate payment: " + e.getMessage());
        }
    }

    public PaymentResponse getByAllocationId(String allocationId) {
        return toResponse(paymentRepository.findByAllocationId(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for allocation: " + allocationId)));
    }

    // ── Stripe webhook: payment_intent.succeeded / payment_intent.payment_failed ──
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new InvalidStateException("Invalid Stripe webhook signature");
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(stripeObject instanceof PaymentIntent intent)) {
            return;
        }

        Payment payment = paymentRepository.findByStripePaymentIntentId(intent.getId()).orElse(null);
        if (payment == null) {
            log.warn("Received Stripe event for unknown PaymentIntent {}", intent.getId());
            return;
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
                publishPaymentConfirmed(payment);
            }
            case "payment_intent.payment_failed" -> {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
            default -> log.info("Ignoring unhandled Stripe event type: {}", event.getType());
        }
    }

    // ── Used by AllocationService to gate delivery confirmation on payment ──
    public boolean isPaid(String allocationId) {
        return paymentRepository.findByAllocationId(allocationId)
                .map(p -> p.getStatus() == PaymentStatus.PAID)
                .orElse(false);
    }

    private void publishPaymentConfirmed(Payment payment) {
        kafkaTemplate.send(TOPIC_PAYMENT_CONFIRMED, PaymentConfirmedEvent.builder()
                        .allocationId(payment.getAllocationId())
                        .dealerId(payment.getDealerId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .build())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment.confirmed for allocation {}: {}", payment.getAllocationId(), ex.getMessage());
                    }
                });
    }

    private long toSmallestUnit(BigDecimal amount) {
        // LKR has no minor unit distinction for Stripe purposes here; Stripe expects the smallest currency unit (cents-equivalent)
        return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .allocationId(payment.getAllocationId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .clientSecret(payment.getClientSecret())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
