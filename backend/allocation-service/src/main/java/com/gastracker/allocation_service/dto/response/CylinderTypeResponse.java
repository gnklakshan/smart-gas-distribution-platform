package com.gastracker.allocation_service.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CylinderTypeResponse {
    private String id;
    private String name;
    private BigDecimal capacityKg;
    private BigDecimal price;
}
