package com.gastracker.inventory_service.dto.response;

import com.gastracker.inventory_service.enums.StockChangeReason;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StockHistoryResponse {
    private String id;
    private String cylinderTypeId;
    private String cylinderTypeName;
    private Integer previousStock;
    private Integer newStock;
    private Integer change;
    private StockChangeReason reason;
    private LocalDateTime changedAt;
}
