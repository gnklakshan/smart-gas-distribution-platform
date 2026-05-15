package com.gastracker.user_service.service;

import com.gastracker.user_service.dao.entity.Dealer;
import com.gastracker.user_service.dao.entity.User;
import com.gastracker.user_service.dao.repository.DealerRepository;
import com.gastracker.user_service.dao.repository.UserRepository;
import com.gastracker.user_service.dto.request.LoginRequest;
import com.gastracker.user_service.dto.request.RegisterDealerRequest;
import com.gastracker.user_service.dto.request.RegisterRequest;
import com.gastracker.user_service.dto.request.UpdateUserRequest;
import com.gastracker.user_service.client.InventoryClient;
import com.gastracker.user_service.dto.response.AuthResponse;
import com.gastracker.user_service.dto.response.FrontendInventoryResponse;
import com.gastracker.user_service.dto.response.NearbyDealerResponse;
import com.gastracker.user_service.dto.response.StockInfo;
import com.gastracker.user_service.dto.response.UserResponse;
import com.gastracker.user_service.enums.Role;
import com.gastracker.user_service.event.DealerRegisteredEvent;
import com.gastracker.user_service.event.UserRegisteredEvent;
import com.gastracker.user_service.exception.DuplicateResourceException;
import com.gastracker.user_service.exception.InvalidCredentialsException;
import com.gastracker.user_service.exception.ResourceNotFoundException;
import com.gastracker.user_service.service.helper.JwtHelper;
import com.gastracker.user_service.service.transformer.UserTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TOPIC_USER_REGISTERED = "user.registered";
    private static final String TOPIC_DEALER_REGISTERED = "dealer.registered";

    private final UserRepository userRepository;
    private final DealerRepository dealerRepository;
    private final JwtHelper jwtHelper;
    private final UserTransformer userTransformer;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final InventoryClient inventoryClient;

    @Transactional
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
                .phone(request.getPhone())
                .role(Role.CITIZEN)
                .build();

        user = userRepository.save(user);
        String token = jwtHelper.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        // Publish Kafka event
        publishUserRegisteredEvent(user);

        return AuthResponse.builder()
                .token(token)
                .user(userTransformer.toResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse registerDealer(RegisterDealerRequest request) {
        String nic = request.getNic().toUpperCase();

        if (userRepository.existsByNic(nic)) {
            throw new DuplicateResourceException("An account already exists for this NIC");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }
        if (dealerRepository.existsByBusinessRegNo(request.getBusinessRegNo())) {
            throw new DuplicateResourceException("Business registration number already in use");
        }

        User user = User.builder()
                .nic(nic)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(Role.DEALER)
                .phone(request.getPhone())
                .build();

        user = userRepository.save(user);

        Dealer dealer = Dealer.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .businessRegNo(request.getBusinessRegNo())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        dealer = dealerRepository.save(dealer);
        user.setDealer(dealer);

        String token = jwtHelper.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        // Publish Kafka events
        publishUserRegisteredEvent(user);
        publishDealerRegisteredEvent(user, dealer);

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

    @Transactional
    public UserResponse updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        if (request.getPhone() != null) user.setPhone(request.getPhone());

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

    @Transactional(readOnly = true)
    public List<NearbyDealerResponse> getNearbyDealers(double lat, double lng, double radiusKm, String authHeader) {
        return dealerRepository.findNearbyDealers(lat, lng, radiusKm).stream()
                .map(row -> {
                    String dealerId = (String) row[0];

                    String userId = (String) row[1];
                    List<StockInfo> stock = List.of();
                    try {
                        stock = inventoryClient.getStockByDealer(authHeader, userId)
                                .stream()
                                .filter(s -> s.getAvailableStock() != null && s.getAvailableStock() > 0)
                                .toList();
                    } catch (Exception e) {
                        log.warn("Could not fetch stock for dealerId={}: {}", dealerId, e.getMessage());
                    }

                    return NearbyDealerResponse.builder()
                            .dealerId(dealerId)
                            .userId(userId)
                            .businessName((String) row[2])
                            .address((String) row[4])
                            .latitude(row[5] != null ? new java.math.BigDecimal(row[5].toString()) : null)
                            .longitude(row[6] != null ? new java.math.BigDecimal(row[6].toString()) : null)
                            .distanceKm(Math.round(((Number) row[7]).doubleValue() * 100.0) / 100.0)
                            .hasStock(!stock.isEmpty())
                            .stock(stock)
                            .build();
                })
                .toList();
    }

    public List<FrontendInventoryResponse> getNearbyInventoryView(double lat, double lng, double radiusKm, String authHeader) {
        return getNearbyDealers(lat, lng, radiusKm, authHeader).stream()
                .map(d -> FrontendInventoryResponse.builder()
                        .id(d.getUserId())
                        .dealerId(d.getUserId())
                        .dealerName(d.getBusinessName())
                        .address(d.getAddress())
                        .latitude(d.getLatitude())
                        .longitude(d.getLongitude())
                        .availableStock(d.getStock().stream()
                                .mapToInt(s -> s.getAvailableStock() != null ? s.getAvailableStock() : 0)
                                .sum())
                        .distanceKm(d.getDistanceKm())
                        .build())
                .toList();
    }

    // ── Kafka event publishers ──────────────────────────────────────────────

    private void publishUserRegisteredEvent(User user) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        kafkaTemplate.send(TOPIC_USER_REGISTERED, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish user.registered for userId={}: {}", user.getId(), ex.getMessage());
                    } else {
                        log.info("Published user.registered for userId={}", user.getId());
                    }
                });
    }

    private void publishDealerRegisteredEvent(User user, Dealer dealer) {
        DealerRegisteredEvent event = DealerRegisteredEvent.builder()
                .userId(user.getId())
                .dealerId(dealer.getId())
                .businessName(dealer.getBusinessName())
                .build();

        kafkaTemplate.send(TOPIC_DEALER_REGISTERED, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish dealer.registered for dealerId={}: {}", dealer.getId(), ex.getMessage());
                    } else {
                        log.info("Published dealer.registered for dealerId={}", dealer.getId());
                    }
                });
    }
}
