package com.gastracker.allocation_service.controller;

import com.gastracker.allocation_service.service.AllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AllocationControllerTest {

    @Mock
    private AllocationService allocationService;

    @InjectMocks
    private AllocationController allocationController;

    @Test
    void testEndpoint_ShouldReturnOkStatus() {
        ResponseEntity<Map<String, String>> response = allocationController.test();
        
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("ok", response.getBody().get("status"));
        assertEquals("allocation-service", response.getBody().get("service"));
    }
}
