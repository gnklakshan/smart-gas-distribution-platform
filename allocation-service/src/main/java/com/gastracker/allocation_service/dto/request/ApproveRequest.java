package com.gastracker.allocation_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproveRequest {

    @NotNull
    @Min(value = 1, message = "Approved quantity must be at least 1")
    private Integer approvedQuantity;
}
