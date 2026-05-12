package com.gastracker.inventory_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStockRequest {

    @NotNull(message = "availableStock is required")
    @Min(value = 0, message = "availableStock must be greater than or equal to 0")
    private Integer availableStock;
}
