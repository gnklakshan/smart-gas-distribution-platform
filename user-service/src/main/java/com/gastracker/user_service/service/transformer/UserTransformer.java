package com.gastracker.user_service.service.transformer;

import com.gastracker.user_service.dao.entity.User;
import com.gastracker.user_service.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserTransformer {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nic(user.getNic())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
