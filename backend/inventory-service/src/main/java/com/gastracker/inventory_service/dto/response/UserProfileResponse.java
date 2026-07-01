package com.gastracker.inventory_service.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileResponse {
    private String id;
    private String name;
    private String role;
    private DealerDetail dealer;

    @Data
    public static class DealerDetail {
        private String dealerId;
        private String businessName;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }
}
