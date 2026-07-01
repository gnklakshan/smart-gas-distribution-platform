package com.gastracker.queue_service.dto.response;

import com.gastracker.queue_service.enums.QueueStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QueueResponse {
    private String id;
    private String userId;
    private String dealerId;
    private String cylinderTypeId;
    private String tokenNumber;
    private QueueStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime fulfilledAt;
}
