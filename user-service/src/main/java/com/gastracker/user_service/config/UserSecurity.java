package com.gastracker.user_service.config;

import com.gastracker.user_service.dao.repository.UserRepository;
import com.gastracker.user_service.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final UserRepository userRepository;

    public boolean isDealer(String id) {
        return userRepository.existsByIdAndRole(id, Role.DEALER);
    }
}
