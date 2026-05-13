package com.gastracker.allocation_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationRejectedEvent {

    private String allocationId;
    private String dealerId;
    private String cylinderTypeId;
    private String reason;
}
