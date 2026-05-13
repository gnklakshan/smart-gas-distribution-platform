package com.gastracker.queue_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinQueueRequest {

    @NotBlank(message = "dealerId is required")
    private String dealerId;

    @NotBlank(message = "cylinderTypeId is required")
    private String cylinderTypeId;
}
