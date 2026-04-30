package com.gastracker.user_service.dto.response;

import com.gastracker.user_service.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private String id;
    private String nic;
    private String email;
    private String name;
    private Role role;
    private String phone;
    private String address;
    private String businessName;
    private String businessRegNo;
    private LocalDateTime createdAt;
}
