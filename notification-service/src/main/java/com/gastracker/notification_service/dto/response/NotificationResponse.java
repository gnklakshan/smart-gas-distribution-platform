package com.gastracker.notification_service.dto.response;

import com.gastracker.notification_service.enums.NotificationChannel;
import com.gastracker.notification_service.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceId;
    private String referenceType;
    private NotificationChannel channel;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
