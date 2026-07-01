package com.gastracker.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @Pattern(
        regexp = "^\\d{9}[VvXx]$|^\\d{12}$",
        message = "Invalid NIC — use old format (e.g. 123456789V) or new format (e.g. 200012345678)"
    )
    private String nic;

    @NotBlank
    private String password;
}
