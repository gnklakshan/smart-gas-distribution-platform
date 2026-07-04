package com.gastracker.inventory_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestControllerTest {

    private final TestController testController = new TestController();

    @Test
    void testEndpoint_ShouldReturnOkStatus() {
        ResponseEntity<Map<String, String>> response = testController.test();
        
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("ok", response.getBody().get("status"));
        assertEquals("inventory-service", response.getBody().get("service"));
    }
}
