package com.gastracker.inventory_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CylinderTypeResponse {
    private String id;
    private String name;
    private BigDecimal capacityKg;
    private BigDecimal price;
}
