package com.gastracker.user_service.service;

import com.gastracker.user_service.dao.entity.User;
import com.gastracker.user_service.dao.repository.UserRepository;
import com.gastracker.user_service.dto.request.LoginRequest;
import com.gastracker.user_service.dto.request.RegisterDealerRequest;
import com.gastracker.user_service.dto.request.RegisterRequest;
import com.gastracker.user_service.dto.request.UpdateUserRequest;
import com.gastracker.user_service.dto.response.AuthResponse;
import com.gastracker.user_service.dto.response.UserResponse;
import com.gastracker.user_service.enums.Role;
import com.gastracker.user_service.exception.DuplicateResourceException;
import com.gastracker.user_service.exception.InvalidCredentialsException;
import com.gastracker.user_service.exception.ResourceNotFoundException;
import com.gastracker.user_service.service.helper.JwtHelper;
import com.gastracker.user_service.service.transformer.UserTransformer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

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
                .role(Role.CITIZEN)
                .build();

        user = userRepository.save(user);
        String token = jwtHelper.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .user(userTransformer.toResponse(user))
                .build();
    }

    public AuthResponse registerDealer(RegisterDealerRequest request) {
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
                .role(Role.DEALER)
                .phone(request.getPhone())
                .address(request.getAddress())
                .businessName(request.getBusinessName())
                .businessRegNo(request.getBusinessRegNo())
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

    public UserResponse updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getBusinessName() != null) user.setBusinessName(request.getBusinessName());

        return userTransformer.toResponse(userRepository.save(user));
    }

    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(userTransformer::toResponse)
                .toList();
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }
}
