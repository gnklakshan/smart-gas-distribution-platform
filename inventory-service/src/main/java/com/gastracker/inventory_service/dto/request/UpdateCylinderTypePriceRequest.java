package com.gastracker.inventory_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateCylinderTypePriceRequest {

    @NotNull(message = "price is required")
    @DecimalMin(value = "0", message = "price must not be negative")
    private BigDecimal price;
}
