package com.gastracker.allocation_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AllocationRequest {

    @NotNull
    @Min(value = 1, message = "Requested quantity must be at least 1")
    private Integer requestedQuantity;

    @NotBlank(message = "Dealer name must not be blank")
    private String dealerName;
}
