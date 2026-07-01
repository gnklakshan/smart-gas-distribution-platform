package com.gastracker.allocation_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectRequest {

    @NotBlank(message = "Rejection reason must not be blank")
    private String reason;
}
