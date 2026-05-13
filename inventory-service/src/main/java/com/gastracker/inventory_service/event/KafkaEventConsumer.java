package com.gastracker.inventory_service.event;

import com.gastracker.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final InventoryService inventoryService;

    /**
     * When an allocation delivery is confirmed, add stock to dealer's inventory.
     */
    @KafkaListener(topics = "allocation.confirmed", groupId = "inventory-service")
    public void handleAllocationConfirmed(Map<String, Object> event) {
        try {
            String dealerId = (String) event.get("dealerId");
            String cylinderTypeId = (String) event.get("cylinderTypeId");
            int quantity = ((Number) event.get("quantity")).intValue();

            log.info("Received allocation.confirmed: dealerId={}, cylinderTypeId={}, quantity={}",
                    dealerId, cylinderTypeId, quantity);

            inventoryService.addStock(dealerId, cylinderTypeId, quantity);
        } catch (Exception e) {
            log.error("Error processing allocation.confirmed event: {}", e.getMessage(), e);
        }
    }

    /**
     * When a queue pickup is completed, subtract stock from dealer's inventory.
     */
    @KafkaListener(topics = "queue.completed", groupId = "inventory-service")
    public void handleQueueCompleted(Map<String, Object> event) {
        try {
            String dealerId = (String) event.get("dealerId");
            String cylinderTypeId = (String) event.get("cylinderTypeId");

            log.info("Received queue.completed: dealerId={}, cylinderTypeId={}", dealerId, cylinderTypeId);

            inventoryService.subtractStock(dealerId, cylinderTypeId, 1);
        } catch (Exception e) {
            log.error("Error processing queue.completed event: {}", e.getMessage(), e);
        }
    }
}
