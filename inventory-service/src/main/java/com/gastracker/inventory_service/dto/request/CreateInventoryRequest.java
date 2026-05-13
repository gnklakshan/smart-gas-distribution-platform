package com.gastracker.inventory_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateInventoryRequest {

    @NotBlank(message = "dealerId is required")
    private String dealerId;

    @NotBlank(message = "cylinderTypeId is required")
    private String cylinderTypeId;

    @NotNull(message = "availableStock is required")
    @Min(value = 0, message = "availableStock must be greater than or equal to 0")
    private Integer availableStock;
}
