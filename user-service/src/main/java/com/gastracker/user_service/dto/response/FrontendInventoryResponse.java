package com.gastracker.user_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FrontendInventoryResponse {
    private String id;
    private String dealerId;
    private String dealerName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer availableStock;
    private Double distanceKm;
}
