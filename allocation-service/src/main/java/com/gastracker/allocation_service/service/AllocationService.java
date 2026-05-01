package com.gastracker.allocation_service.service;

import com.gastracker.allocation_service.dao.entity.Allocation;
import com.gastracker.allocation_service.dao.repository.AllocationRepository;
import com.gastracker.allocation_service.dto.request.AllocationRequest;
import com.gastracker.allocation_service.dto.request.ApproveRequest;
import com.gastracker.allocation_service.dto.request.RejectRequest;
import com.gastracker.allocation_service.dto.response.AllocationResponse;
import com.gastracker.allocation_service.enums.AllocationStatus;
import com.gastracker.allocation_service.event.AllocationConfirmedEvent;
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

    private static final String TOPIC_ALLOCATION_CONFIRMED = "allocation.confirmed";

    private final AllocationRepository allocationRepository;
    private final AllocationTransformer allocationTransformer;
    private final KafkaTemplate<String, AllocationConfirmedEvent> kafkaTemplate;

    // ── DEALER: submit a new gas allocation request ────────────────────────
    @Transactional
    public AllocationResponse requestAllocation(String dealerId, AllocationRequest request) {
        Allocation allocation = Allocation.builder()
                .dealerId(dealerId)
                .dealerName(request.getDealerName())
                .requestedQuantity(request.getRequestedQuantity())
                .status(AllocationStatus.PENDING)
                .build();

        return allocationTransformer.toResponse(allocationRepository.save(allocation));
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

        return allocationTransformer.toResponse(allocationRepository.save(allocation));
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

        return allocationTransformer.toResponse(allocationRepository.save(allocation));
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

        AllocationConfirmedEvent event = AllocationConfirmedEvent.builder()
                .allocationId(allocation.getId())
                .dealerId(allocation.getDealerId())
                .quantity(allocation.getApprovedQuantity())
                .build();

        kafkaTemplate.send(TOPIC_ALLOCATION_CONFIRMED, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish allocation.confirmed for allocationId={}: {}", id, ex.getMessage());
                    } else {
                        log.info("Published allocation.confirmed for allocationId={} dealerId={} qty={}",
                                id, allocation.getDealerId(), allocation.getApprovedQuantity());
                    }
                });

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

    // ── Private helper ─────────────────────────────────────────────────────
    private Allocation findById(String id) {
        return allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));
    }
}
