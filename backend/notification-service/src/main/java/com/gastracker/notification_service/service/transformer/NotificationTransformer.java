package com.gastracker.notification_service.service.transformer;

import com.gastracker.notification_service.dao.entity.Notification;
import com.gastracker.notification_service.dto.response.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationTransformer {

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .channel(notification.getChannel())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
