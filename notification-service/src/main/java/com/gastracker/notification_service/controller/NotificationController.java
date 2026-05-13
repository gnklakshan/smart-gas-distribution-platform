package com.gastracker.notification_service.controller;

import com.gastracker.notification_service.dto.response.NotificationResponse;
import com.gastracker.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── Get all own notifications ─────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    // ── Get unread notifications ──────────────────────────────────────────
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    // ── Get unread count ──────────────────────────────────────────────────
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(userId)));
    }

    // ── Mark single notification as read ──────────────────────────────────
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String id,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    // ── Mark all notifications as read ────────────────────────────────────
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
