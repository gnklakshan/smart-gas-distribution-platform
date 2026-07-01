package com.gastracker.queue_service.service.transformer;

import com.gastracker.queue_service.dao.entity.CitizenQueue;
import com.gastracker.queue_service.dto.response.QueueResponse;
import org.springframework.stereotype.Component;

@Component
public class QueueTransformer {

    public QueueResponse toResponse(CitizenQueue queue) {
        return QueueResponse.builder()
                .id(queue.getId())
                .userId(queue.getUserId())
                .dealerId(queue.getDealerId())
                .cylinderTypeId(queue.getCylinderTypeId())
                .tokenNumber(queue.getTokenNumber())
                .status(queue.getStatus())
                .requestedAt(queue.getRequestedAt())
                .fulfilledAt(queue.getFulfilledAt())
                .build();
    }
}
