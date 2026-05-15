package com.gastracker.user_service.client;

import com.gastracker.user_service.dto.response.StockInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/v1/inventory/dealer/{dealerId}")
    List<StockInfo> getStockByDealer(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("dealerId") String dealerId
    );
}
