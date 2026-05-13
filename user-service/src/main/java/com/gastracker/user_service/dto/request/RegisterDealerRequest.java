package com.gastracker.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegisterDealerRequest {

    @NotBlank
    @Pattern(
        regexp = "^\\d{9}[VvXx]$|^\\d{12}$",
        message = "Invalid NIC — use old format (e.g. 123456789V) or new format (e.g. 200012345678)"
    )
    private String nic;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    @NotBlank
    private String address;

    @NotBlank
    private String businessName;

    @NotBlank
    private String businessRegNo;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
