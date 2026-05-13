package com.gastracker.inventory_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryResponse {
    private String id;
    private String dealerId;
    private String cylinderTypeId;
    private String cylinderTypeName;
    private Integer availableStock;
    private LocalDateTime lastUpdated;
}
