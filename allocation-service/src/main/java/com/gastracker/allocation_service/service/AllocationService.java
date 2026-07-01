package com.gastracker.allocation_service.service;

import com.gastracker.allocation_service.dao.entity.Allocation;
import com.gastracker.allocation_service.dao.repository.AllocationRepository;
import com.gastracker.allocation_service.dto.request.AllocationRequest;
import com.gastracker.allocation_service.dto.request.ApproveRequest;
import com.gastracker.allocation_service.dto.request.RejectRequest;
import com.gastracker.allocation_service.dto.response.AllocationAnalyticsResponse;
import com.gastracker.allocation_service.dto.response.AllocationResponse;
import com.gastracker.allocation_service.enums.AllocationStatus;
import com.gastracker.allocation_service.event.AllocationApprovedEvent;
import com.gastracker.allocation_service.event.AllocationConfirmedEvent;
import com.gastracker.allocation_service.event.AllocationRejectedEvent;
import com.gastracker.allocation_service.event.AllocationRequestedEvent;
import com.gastracker.allocation_service.exception.InvalidStateException;
import com.gastracker.allocation_service.exception.ResourceNotFoundException;
import com.gastracker.allocation_service.service.transformer.AllocationTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationService {

    private static final String TOPIC_ALLOCATION_REQUESTED = "allocation.requested";
    private static final String TOPIC_ALLOCATION_APPROVED = "allocation.approved";
    private static final String TOPIC_ALLOCATION_REJECTED = "allocation.rejected";
    private static final String TOPIC_ALLOCATION_CONFIRMED = "allocation.confirmed";

    private final AllocationRepository allocationRepository;
    private final AllocationTransformer allocationTransformer;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── DEALER: submit a new gas allocation request ────────────────────────
    @Transactional
    public AllocationResponse requestAllocation(String dealerId, AllocationRequest request) {
        Allocation allocation = Allocation.builder()
                .dealerId(dealerId)
                .cylinderTypeId(request.getCylinderTypeId())
                .requestedQuantity(request.getRequestedQuantity())
                .status(AllocationStatus.PENDING)
                .build();

        allocation = allocationRepository.save(allocation);

        // Publish Kafka event
        publishEvent(TOPIC_ALLOCATION_REQUESTED, AllocationRequestedEvent.builder()
                .allocationId(allocation.getId())
                .dealerId(allocation.getDealerId())
                .cylinderTypeId(allocation.getCylinderTypeId())
                .requestedQuantity(allocation.getRequestedQuantity())
                .build());

        return allocationTransformer.toResponse(allocation);
    }

    // ── ADMIN: approve a pending allocation ────────────────────────────────
    @Transactional
    public AllocationResponse approveAllocation(String id, ApproveRequest request) {
        Allocation allocation = findById(id);

        if (allocation.getStatus() != AllocationStatus.PENDING) {
            throw new InvalidStateException("Only PENDING allocations can be approved. Current status: " + allocation.getStatus());
        }

        allocation.setApprovedQuantity(request.getApprovedQuantity());
        allocation.setStatus(AllocationStatus.APPROVED);
        allocation.setResolvedAt(LocalDateTime.now());
        allocation = allocationRepository.save(allocation);

        // Publish Kafka event
        publishEvent(TOPIC_ALLOCATION_APPROVED, AllocationApprovedEvent.builder()
                .allocationId(allocation.getId())
                .dealerId(allocation.getDealerId())
                .cylinderTypeId(allocation.getCylinderTypeId())
                .approvedQuantity(allocation.getApprovedQuantity())
                .build());

        return allocationTransformer.toResponse(allocation);
    }

    // ── ADMIN: reject a pending allocation ────────────────────────────────
    @Transactional
    public AllocationResponse rejectAllocation(String id, RejectRequest request) {
        Allocation allocation = findById(id);

        if (allocation.getStatus() != AllocationStatus.PENDING) {
            throw new InvalidStateException("Only PENDING allocations can be rejected. Current status: " + allocation.getStatus());
        }

        allocation.setRejectionReason(request.getReason());
        allocation.setStatus(AllocationStatus.REJECTED);
        allocation.setResolvedAt(LocalDateTime.now());
        allocation = allocationRepository.save(allocation);

        // Publish Kafka event
        publishEvent(TOPIC_ALLOCATION_REJECTED, AllocationRejectedEvent.builder()
                .allocationId(allocation.getId())
                .dealerId(allocation.getDealerId())
                .cylinderTypeId(allocation.getCylinderTypeId())
                .reason(request.getReason())
                .build());

        return allocationTransformer.toResponse(allocation);
    }

    // ── DEALER: confirm physical delivery — publishes Kafka event ──────────
    @Transactional
    public AllocationResponse confirmDelivery(String id, String dealerId) {
        Allocation allocation = findById(id);

        if (!allocation.getDealerId().equals(dealerId)) {
            throw new AccessDeniedException("You do not own this allocation");
        }
        if (allocation.getStatus() != AllocationStatus.APPROVED) {
            throw new InvalidStateException("Only APPROVED allocations can be confirmed. Current status: " + allocation.getStatus());
        }

        allocation.setStatus(AllocationStatus.DELIVERED);
        allocation.setDeliveredAt(LocalDateTime.now());
        allocationRepository.save(allocation);

        // Publish Kafka event — consumed by inventory-service to add stock
        publishEvent(TOPIC_ALLOCATION_CONFIRMED, AllocationConfirmedEvent.builder()
                .allocationId(allocation.getId())
                .dealerId(allocation.getDealerId())
                .cylinderTypeId(allocation.getCylinderTypeId())
                .quantity(allocation.getApprovedQuantity())
                .build());

        return allocationTransformer.toResponse(allocation);
    }

    // ── READ queries ───────────────────────────────────────────────────────
    public AllocationResponse getById(String id) {
        return allocationTransformer.toResponse(findById(id));
    }

    public List<AllocationResponse> getPending() {
        return allocationRepository.findByStatus(AllocationStatus.PENDING)
                .stream().map(allocationTransformer::toResponse).toList();
    }

    public List<AllocationResponse> getAll(AllocationStatus status) {
        List<Allocation> results = (status != null)
                ? allocationRepository.findByStatus(status)
                : allocationRepository.findAll();
        return results.stream().map(allocationTransformer::toResponse).toList();
    }

    public List<AllocationResponse> getByDealer(String dealerId) {
        return allocationRepository.findByDealerIdOrderByRequestedAtDesc(dealerId)
                .stream().map(allocationTransformer::toResponse).toList();
    }

    // ── DEALER: allocation analytics + fair-distribution comparison ────────
    public AllocationAnalyticsResponse getAnalytics(String dealerId) {
        long totalRequested = allocationRepository.countByDealerId(dealerId);
        long totalApproved = allocationRepository.countByDealerIdAndStatusIn(dealerId, List.of(AllocationStatus.APPROVED, AllocationStatus.DELIVERED));
        long totalRejected = allocationRepository.countByDealerIdAndStatus(dealerId, AllocationStatus.REJECTED);
        long pendingCount = allocationRepository.countByDealerIdAndStatus(dealerId, AllocationStatus.PENDING);
        double fulfillmentRatePct = totalRequested == 0 ? 0 : (totalApproved * 100.0) / totalRequested;

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        int last30DaysApprovedQty = allocationRepository.sumApprovedQuantitySince(dealerId, since);

        List<Object[]> perDealerSums = allocationRepository.sumApprovedQuantitySinceGroupedByDealer(since);
        double platformAvg = perDealerSums.isEmpty() ? 0 : perDealerSums.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .average()
                .orElse(0);

        return AllocationAnalyticsResponse.builder()
                .totalRequested(totalRequested)
                .totalApproved(totalApproved)
                .totalRejected(totalRejected)
                .pendingCount(pendingCount)
                .fulfillmentRatePct(Math.round(fulfillmentRatePct * 10.0) / 10.0)
                .last30DaysApprovedQty(last30DaysApprovedQty)
                .platformAvg30DaysApprovedQty(Math.round(platformAvg * 10.0) / 10.0)
                .build();
    }

    // ── Private helpers ─────────────────────────────────────────────────────
    private Allocation findById(String id) {
        return allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));
    }

    private void publishEvent(String topic, Object event) {
        kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to {}: {}", topic, ex.getMessage());
                    } else {
                        log.info("Published event to {}", topic);
                    }
                });
    }
}
