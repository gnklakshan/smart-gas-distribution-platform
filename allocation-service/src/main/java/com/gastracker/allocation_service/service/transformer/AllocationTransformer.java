package com.gastracker.allocation_service.service.transformer;

import com.gastracker.allocation_service.dao.entity.Allocation;
import com.gastracker.allocation_service.dto.response.AllocationResponse;
import org.springframework.stereotype.Component;

@Component
public class AllocationTransformer {

    public AllocationResponse toResponse(Allocation allocation) {
        return AllocationResponse.builder()
                .id(allocation.getId())
                .dealerId(allocation.getDealerId())
                .dealerName(allocation.getDealerName())
                .requestedQuantity(allocation.getRequestedQuantity())
                .approvedQuantity(allocation.getApprovedQuantity())
                .status(allocation.getStatus())
                .rejectionReason(allocation.getRejectionReason())
                .requestedAt(allocation.getRequestedAt())
                .resolvedAt(allocation.getResolvedAt())
                .deliveredAt(allocation.getDeliveredAt())
                .build();
    }
}
