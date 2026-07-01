package com.gastracker.inventory_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
public class TestController {

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "inventory-service",
                "message", "Inventory Service is running"
        ));
    }
}
