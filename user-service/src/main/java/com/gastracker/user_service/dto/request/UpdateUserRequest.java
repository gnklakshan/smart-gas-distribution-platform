package com.gastracker.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    private String phone;
}
