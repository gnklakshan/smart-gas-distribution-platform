package com.gastracker.queue_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueueAnalyticsResponse {
    private long waitingCount;
    private long readyCount;
    private long completedToday;
    private Double avgWaitMinutes;
}
