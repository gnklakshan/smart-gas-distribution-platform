package com.gastracker.notification_service.service;

import com.gastracker.notification_service.dao.entity.Notification;
import com.gastracker.notification_service.dao.repository.NotificationRepository;
import com.gastracker.notification_service.dto.response.NotificationResponse;
import com.gastracker.notification_service.enums.NotificationChannel;
import com.gastracker.notification_service.enums.NotificationType;
import com.gastracker.notification_service.exception.ResourceNotFoundException;
import com.gastracker.notification_service.service.transformer.NotificationTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTransformer notificationTransformer;

    /**
     * Create and persist a notification.
     */
    @Transactional
    public void createNotification(String userId, NotificationType type, String title,
                                    String message, String referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Created {} notification for user={}", type, userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(notificationTransformer::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(notificationTransformer::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(String id, String userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        return notificationTransformer.toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(String userId) {
        int count = notificationRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read for user={}", count, userId);
    }
}
