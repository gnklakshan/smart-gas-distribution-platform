package com.gastracker.allocation_service.client;

import com.gastracker.allocation_service.dto.response.CylinderTypeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/v1/cylinder-types/{id}")
    CylinderTypeResponse getCylinderTypeById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("id") String id
    );
}
