package com.gastracker.inventory_service.client;

import com.gastracker.inventory_service.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/v1/users/{id}")
    UserProfileResponse getUserById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("id") String id
    );
}
