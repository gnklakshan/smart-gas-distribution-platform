package com.gastracker.allocation_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AllocationAnalyticsResponse {
    private long totalRequested;
    private long totalApproved;
    private long totalRejected;
    private long pendingCount;
    private double fulfillmentRatePct;
    private int last30DaysApprovedQty;
    private double platformAvg30DaysApprovedQty;
}
