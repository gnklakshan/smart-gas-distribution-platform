package com.gastracker.user_service.dto.response;

import lombok.Data;

@Data
public class StockInfo {
    private String cylinderTypeName;
    private Integer availableStock;
}
