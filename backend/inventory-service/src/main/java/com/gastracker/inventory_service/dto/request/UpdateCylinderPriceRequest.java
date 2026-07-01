package com.gastracker.inventory_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateCylinderPriceRequest {

    @NotNull(message = "price is required")
    private BigDecimal price;
}
