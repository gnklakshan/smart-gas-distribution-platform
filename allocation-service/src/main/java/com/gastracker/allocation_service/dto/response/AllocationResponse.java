package com.gastracker.allocation_service.dto.response;

import com.gastracker.allocation_service.enums.AllocationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AllocationResponse {

    private String id;
    private String dealerId;
    private String dealerName;
    private Integer requestedQuantity;
    private Integer approvedQuantity;
    private AllocationStatus status;
    private String rejectionReason;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime deliveredAt;
}
