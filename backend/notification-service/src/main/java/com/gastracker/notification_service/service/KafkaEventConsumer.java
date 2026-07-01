package com.gastracker.notification_service.service;

import com.gastracker.notification_service.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final NotificationService notificationService;

    // ── User Events ─────────────────────────────────────────────────────────

    @KafkaListener(topics = "user.registered", groupId = "notification-service")
    public void handleUserRegistered(Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String name = (String) event.get("name");

            notificationService.createNotification(
                    userId,
                    NotificationType.WELCOME,
                    "Welcome to GasTracker!",
                    "Hi " + name + ", welcome to the Smart Gas Distribution Platform. You can now find nearby dealers and join queues for gas cylinders.",
                    userId,
                    "USER"
            );
        } catch (Exception e) {
            log.error("Error processing user.registered event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "dealer.registered", groupId = "notification-service")
    public void handleDealerRegistered(Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String businessName = (String) event.get("businessName");

            notificationService.createNotification(
                    userId,
                    NotificationType.DEALER_REGISTERED,
                    "Dealer Account Created",
                    "Your dealer account for '" + businessName + "' has been successfully created. You can now manage your inventory and receive allocation requests.",
                    (String) event.get("dealerId"),
                    "USER"
            );
        } catch (Exception e) {
            log.error("Error processing dealer.registered event: {}", e.getMessage(), e);
        }
    }

    // ── Allocation Events ───────────────────────────────────────────────────

    @KafkaListener(topics = "allocation.requested", groupId = "notification-service")
    public void handleAllocationRequested(Map<String, Object> event) {
        try {
            String dealerId = (String) event.get("dealerId");
            String allocationId = (String) event.get("allocationId");
            Number qty = (Number) event.get("requestedQuantity");

            notificationService.createNotification(
                    dealerId,
                    NotificationType.ALLOCATION_REQUESTED,
                    "Allocation Request Submitted",
                    "Your allocation request for " + qty + " cylinders has been submitted and is pending admin approval.",
                    allocationId,
                    "ALLOCATION"
            );
        } catch (Exception e) {
            log.error("Error processing allocation.requested event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "allocation.approved", groupId = "notification-service")
    public void handleAllocationApproved(Map<String, Object> event) {
        try {
            String dealerId = (String) event.get("dealerId");
            String allocationId = (String) event.get("allocationId");
            Number qty = (Number) event.get("approvedQuantity");

            notificationService.createNotification(
                    dealerId,
                    NotificationType.ALLOCATION_APPROVED,
                    "Allocation Approved",
                    "Your allocation request has been approved for " + qty + " cylinders. Please confirm delivery once received.",
                    allocationId,
                    "ALLOCATION"
            );
        } catch (Exception e) {
            log.error("Error processing allocation.approved event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "allocation.rejected", groupId = "notification-service")
    public void handleAllocationRejected(Map<String, Object> event) {
        try {
            String dealerId = (String) event.get("dealerId");
            String allocationId = (String) event.get("allocationId");
            String reason = (String) event.get("reason");

            notificationService.createNotification(
                    dealerId,
                    NotificationType.ALLOCATION_REJECTED,
                    "Allocation Rejected",
                    "Your allocation request has been rejected. Reason: " + reason,
                    allocationId,
                    "ALLOCATION"
            );
        } catch (Exception e) {
            log.error("Error processing allocation.rejected event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "allocation.confirmed", groupId = "notification-service")
    public void handleAllocationConfirmed(Map<String, Object> event) {
        try {
            String dealerId = (String) event.get("dealerId");
            String allocationId = (String) event.get("allocationId");
            Number qty = (Number) event.get("quantity");

            notificationService.createNotification(
                    dealerId,
                    NotificationType.ALLOCATION_CONFIRMED,
                    "Delivery Confirmed",
                    "You have confirmed delivery of " + qty + " cylinders. Your inventory has been updated.",
                    allocationId,
                    "ALLOCATION"
            );
        } catch (Exception e) {
            log.error("Error processing allocation.confirmed event: {}", e.getMessage(), e);
        }
    }

    // ── Queue Events ────────────────────────────────────────────────────────

    @KafkaListener(topics = "queue.joined", groupId = "notification-service")
    public void handleQueueJoined(Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String dealerId = (String) event.get("dealerId");
            String queueId = (String) event.get("queueId");
            String tokenNumber = (String) event.get("tokenNumber");

            // Notify citizen
            notificationService.createNotification(
                    userId,
                    NotificationType.QUEUE_JOINED,
                    "Queue Joined",
                    "You have joined the queue. Your token number is: " + tokenNumber + ". We'll notify you when your cylinder is ready for pickup.",
                    queueId,
                    "QUEUE"
            );

            // Notify dealer
            notificationService.createNotification(
                    dealerId,
                    NotificationType.QUEUE_JOINED,
                    "New Queue Entry",
                    "A citizen has joined your queue with token " + tokenNumber + ".",
                    queueId,
                    "QUEUE"
            );
        } catch (Exception e) {
            log.error("Error processing queue.joined event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "queue.ready", groupId = "notification-service")
    public void handleQueueReady(Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String queueId = (String) event.get("queueId");
            String tokenNumber = (String) event.get("tokenNumber");

            notificationService.createNotification(
                    userId,
                    NotificationType.QUEUE_READY,
                    "Ready for Pickup!",
                    "Your gas cylinder is ready for pickup! Token: " + tokenNumber + ". Please visit the dealer to collect your cylinder.",
                    queueId,
                    "QUEUE"
            );
        } catch (Exception e) {
            log.error("Error processing queue.ready event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "queue.completed", groupId = "notification-service")
    public void handleQueueCompleted(Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String queueId = (String) event.get("queueId");
            String tokenNumber = (String) event.get("tokenNumber");

            notificationService.createNotification(
                    userId,
                    NotificationType.QUEUE_COMPLETED,
                    "Pickup Completed",
                    "Your gas cylinder pickup with token " + tokenNumber + " has been completed. Thank you!",
                    queueId,
                    "QUEUE"
            );
        } catch (Exception e) {
            log.error("Error processing queue.completed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "queue.cancelled", groupId = "notification-service")
    public void handleQueueCancelled(Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String queueId = (String) event.get("queueId");
            String tokenNumber = (String) event.get("tokenNumber");

            notificationService.createNotification(
                    userId,
                    NotificationType.QUEUE_CANCELLED,
                    "Queue Entry Cancelled",
                    "Your queue entry with token " + tokenNumber + " has been cancelled.",
                    queueId,
                    "QUEUE"
            );
        } catch (Exception e) {
            log.error("Error processing queue.cancelled event: {}", e.getMessage(), e);
        }
    }
}
