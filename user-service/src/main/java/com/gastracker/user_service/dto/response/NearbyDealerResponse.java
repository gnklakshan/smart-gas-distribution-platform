package com.gastracker.user_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class NearbyDealerResponse {
    private String dealerId;
    private String userId;
    private String businessName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private double distanceKm;
    private boolean hasStock;
    private List<StockInfo> stock;
}
