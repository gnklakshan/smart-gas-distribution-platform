package com.gastracker.user_service.service;

import com.gastracker.user_service.dao.entity.User;
import com.gastracker.user_service.dao.repository.UserRepository;
import com.gastracker.user_service.dto.request.LoginRequest;
import com.gastracker.user_service.dto.request.RegisterRequest;
import com.gastracker.user_service.dto.response.AuthResponse;
import com.gastracker.user_service.dto.response.UserResponse;
import com.gastracker.user_service.service.helper.JwtHelper;
import com.gastracker.user_service.service.transformer.UserTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtHelper jwtHelper;
    private final UserTransformer userTransformer;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        String nic = request.getNic().toUpperCase();

        if (userRepository.existsByNic(nic)) {
            throw new RuntimeException("An account already exists for this NIC");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .nic(nic)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .build();

        user = userRepository.save(user);
        String token = jwtHelper.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .user(userTransformer.toResponse(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String nic = request.getNic().toUpperCase();
        User user = userRepository.findByNic(nic)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtHelper.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .user(userTransformer.toResponse(user))
                .build();
    }

    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userTransformer.toResponse(user);
    }
}
