package com.gastracker.inventory_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCylinderTypeRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "capacityKg is required")
    private BigDecimal capacityKg;
}
