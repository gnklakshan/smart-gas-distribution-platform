package com.gastracker.user_service.controller;

import com.gastracker.user_service.dto.request.LoginRequest;
import com.gastracker.user_service.dto.request.RegisterDealerRequest;
import com.gastracker.user_service.dto.request.RegisterRequest;
import com.gastracker.user_service.dto.request.UpdateUserRequest;
import com.gastracker.user_service.dto.response.AuthResponse;
import com.gastracker.user_service.dto.response.NearbyDealerResponse;
import com.gastracker.user_service.dto.response.UserResponse;
import com.gastracker.user_service.enums.Role;
import com.gastracker.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Public: citizen self-registration
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    // ADMIN only: register a new dealer with business details
    @PostMapping("/register/dealer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> registerDealer(@Valid @RequestBody RegisterDealerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerDealer(request));
    }

    // Public
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    // Authenticated: returns own profile without needing to know the UUID
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    // Authenticated: ADMIN can fetch anyone; CITIZEN/DEALER can only fetch their own
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // Authenticated: ADMIN can update anyone; CITIZEN/DEALER can only update their own
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // ADMIN only: list all users by role
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    // Authenticated: search dealers near a GPS coordinate with stock availability
    @GetMapping("/dealers/nearby")
    public ResponseEntity<List<NearbyDealerResponse>> getNearbyDealers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(userService.getNearbyDealers(lat, lng, radius, authHeader));
    }

    // ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
