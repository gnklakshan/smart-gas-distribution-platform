package com.gastracker.user_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealerRegisteredEvent {
    private String userId;
    private String dealerId;
    private String businessName;
}
