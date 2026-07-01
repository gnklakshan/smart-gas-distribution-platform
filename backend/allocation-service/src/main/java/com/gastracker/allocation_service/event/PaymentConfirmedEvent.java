package com.gastracker.allocation_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmedEvent {
    private String allocationId;
    private String dealerId;
    private BigDecimal amount;
    private String currency;
}
