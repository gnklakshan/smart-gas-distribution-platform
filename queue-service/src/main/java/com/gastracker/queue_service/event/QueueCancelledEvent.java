package com.gastracker.queue_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QueueCancelledEvent {
    private String queueId;
    private String userId;
    private String dealerId;
    private String tokenNumber;
}
