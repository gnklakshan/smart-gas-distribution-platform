package com.gastracker.user_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DealerInfo {
    private String dealerId;
    private String businessName;
    private String businessRegNo;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
