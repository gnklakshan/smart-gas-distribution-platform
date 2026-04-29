package com.gastracker.user_service.service;

import com.gastracker.user_service.dao.entity.User;
import com.gastracker.user_service.dao.repository.UserRepository;
import com.gastracker.user_service.dto.request.LoginRequest;
import com.gastracker.user_service.dto.request.RegisterRequest;
import com.gastracker.user_service.dto.response.AuthResponse;
import com.gastracker.user_service.dto.response.UserResponse;
import com.gastracker.user_service.service.helper.JwtHelper;
import com.gastracker.user_service.service.transformer.UserTransformer;
import com.gastracker.user_service.exception.DuplicateResourceException;
import com.gastracker.user_service.exception.InvalidCredentialsException;
import com.gastracker.user_service.exception.ResourceNotFoundException;
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
            throw new DuplicateResourceException("An account already exists for this NIC");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
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
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtHelper.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .user(userTransformer.toResponse(user))
                .build();
    }

    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userTransformer.toResponse(user);
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }
}
