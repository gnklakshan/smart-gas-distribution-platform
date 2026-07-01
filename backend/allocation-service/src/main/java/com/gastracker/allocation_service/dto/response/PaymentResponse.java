package com.gastracker.allocation_service.dto.response;

import com.gastracker.allocation_service.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String id;
    private String allocationId;
    private BigDecimal amount;
    private String currency;
    private String clientSecret;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
