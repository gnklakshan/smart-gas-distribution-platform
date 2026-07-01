package com.gastracker.user_service.controller;

import com.gastracker.user_service.dto.response.FrontendInventoryResponse;
import com.gastracker.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryViewController {

    private final UserService userService;

    @GetMapping("/nearby")
    public ResponseEntity<List<FrontendInventoryResponse>> getNearbyInventory(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(userService.getNearbyInventoryView(lat, lng, radius, authHeader));
    }
}
